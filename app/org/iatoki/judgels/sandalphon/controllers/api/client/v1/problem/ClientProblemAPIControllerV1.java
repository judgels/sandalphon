package org.iatoki.judgels.sandalphon.controllers.api.client.v1.problem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.apis.JudgelsAPIForbiddenException;
import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapterRegistry;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.ProblemStatementRenderRequestV1;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.ProblemV1;
import org.iatoki.judgels.sandalphon.controllers.api.util.TOTPUtils;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.statement.bundleStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public final class ClientProblemAPIControllerV1 extends AbstractJudgelsAPIController {

    private final BundleItemService bundleItemService;
    private final ClientService clientService;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ClientProblemAPIControllerV1(BundleItemService bundleItemService, ClientService clientService, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.bundleItemService = bundleItemService;
        this.clientService = clientService;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    public Result findProblemByJid(String problemJid) {
        authenticateAsJudgelsAppClient(clientService);

        if (!problemService.problemExistsByJid(problemJid)) {
            throw new JudgelsAPINotFoundException();
        }

        try {
            Problem problem = problemService.findProblemByJid(problemJid);

            ProblemV1 responseBody = new ProblemV1();

            responseBody.jid = problem.getJid();
            responseBody.slug = problem.getSlug();
            responseBody.defaultLanguage = problemService.getDefaultLanguage(null, problemJid);
            responseBody.titlesByLanguage = problemService.getTitlesByLanguage(null, problemJid);

            return okAsJson(responseBody);
        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }

    public Result renderMediaByJid(String problemJid, String mediaFilename) {
        String mediaUrl = problemService.getStatementMediaFileURL(null, problemJid, mediaFilename);
        try {
            new URL(mediaUrl);
            return temporaryRedirect(mediaUrl);
        } catch (MalformedURLException e) {
            File imageFile = new File(mediaUrl);
            if (!imageFile.exists()) {
                return notFound();
            }

            return okAsImage(imageFile);
        }
    }

    @Transactional(readOnly = true)
    public Result renderStatement(String problemJid) {
        ProblemStatementRenderRequestV1 requestBody = parseRequestBodyAsUrlFormEncoded(ProblemStatementRenderRequestV1.class);

        if (!problemService.problemExistsByJid(problemJid)) {
            throw new JudgelsAPINotFoundException();
        }
        if (!clientService.clientExistsByJid(requestBody.clientJid)) {
            throw new JudgelsAPIForbiddenException("Client not exists");
        }
        if (!clientService.isClientAuthorizedForProblem(requestBody.problemJid, requestBody.clientJid)) {
            throw new JudgelsAPIForbiddenException("Client not authorized to view problem");
        }

        Problem problem = problemService.findProblemByJid(problemJid);
        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(requestBody.clientJid, problemJid);

        if (!TOTPUtils.match(clientProblem.getSecret(), requestBody.totpCode)) {
            throw new JudgelsAPIForbiddenException("TOTP code mismatch");
        }

        try {
            Map<String, StatementLanguageStatus> availableStatementLanguages = problemService.getAvailableLanguages(null, problem.getJid());

            String statementLanguage = requestBody.statementLanguage;
            if (!availableStatementLanguages.containsKey(statementLanguage) || availableStatementLanguages.get(statementLanguage) == StatementLanguageStatus.DISABLED) {
                statementLanguage = problemService.getDefaultLanguage(null, problemJid);
            }

            ProblemStatement statement = problemService.getStatement(null, problemJid, statementLanguage);

            Set<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet());
            Set<String> allowedGradingLanguages;
            if (requestBody.allowedGradingLanguages.isEmpty()) {
                allowedGradingLanguages = ImmutableSet.of();
            } else {
                allowedGradingLanguages = ImmutableSet.copyOf(requestBody.allowedGradingLanguages.split(","));
            }

            if (problem.getType().equals(ProblemType.PROGRAMMING)) {
                return renderProgrammingProblemStatement(problem, statement, allowedStatementLanguages, statementLanguage, requestBody.switchStatementLanguageUrl, requestBody.postSubmitUrl, requestBody.reasonNotAllowedToSubmit, allowedGradingLanguages);
            } else if (problem.getType().equals(ProblemType.BUNDLE)) {
                return renderBundleProblemStatement(problem, statement, allowedStatementLanguages, statementLanguage, requestBody.switchStatementLanguageUrl, requestBody.postSubmitUrl, requestBody.reasonNotAllowedToSubmit);
            } else {
                return notFound();
            }
        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }

    private Result renderProgrammingProblemStatement(Problem problem, ProblemStatement statement, Set<String> allowedStatementLanguages, String statementLanguage, String switchStatementLanguageUrl, String postSubmitUrl, String reasonNotAllowedToSubmit, Set<String> allowedGradingLanguages) {
        String gradingEngine;
        try {
            gradingEngine = programmingProblemService.getGradingEngine(null, problem.getJid());
        } catch (IOException e) {
            gradingEngine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }

        LanguageRestriction problemLanguageRestriction;
        try {
            problemLanguageRestriction = programmingProblemService.getLanguageRestriction(null, problem.getJid());
        } catch (IOException e) {
            problemLanguageRestriction = LanguageRestriction.defaultRestriction();
        }

        LanguageRestriction clientLanguageRestriction = new LanguageRestriction(allowedGradingLanguages);

        Set<String> finalAllowedGradingLanguages = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(problemLanguageRestriction, clientLanguageRestriction));

        GradingConfig config;
        try {
            config = programmingProblemService.getGradingConfig(null, problem.getJid());
        } catch (IOException e) {
            config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
        }

        Html statementHtml = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(gradingEngine).renderViewStatement(postSubmitUrl, statement, config, gradingEngine, finalAllowedGradingLanguages, reasonNotAllowedToSubmit);
        if (switchStatementLanguageUrl != null) {
            statementHtml = statementLanguageSelectionLayout.render(switchStatementLanguageUrl, allowedStatementLanguages, statementLanguage, statementHtml);
        }

        return ok(statementHtml);
    }

    private Result renderBundleProblemStatement(Problem problem, ProblemStatement statement, Set<String> allowedStatementLanguages, String statementLanguage, String switchLanguageUrl, String postSubmitUrl, String reasonNotAllowedToSubmit) throws IOException {
        List<BundleItem> bundleItemList = bundleItemService.getBundleItemsInProblemWithClone(problem.getJid(), IdentityUtils.getUserJid());
        ImmutableList.Builder<Html> htmlBuilder = ImmutableList.builder();
        for (BundleItem bundleItem : bundleItemList) {
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfInProblemWithCloneByJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), statementLanguage)));
        }

        Html statementHtml = bundleStatementView.render(postSubmitUrl, statement, htmlBuilder.build(), reasonNotAllowedToSubmit);
        if (switchLanguageUrl != null) {
            statementHtml = statementLanguageSelectionLayout.render(switchLanguageUrl, allowedStatementLanguages, statementLanguage, statementHtml);
        }

        return ok(statementHtml);
    }
}
