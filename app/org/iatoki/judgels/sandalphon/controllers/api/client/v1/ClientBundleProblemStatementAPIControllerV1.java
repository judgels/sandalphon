package org.iatoki.judgels.sandalphon.controllers.api.client.v1;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.apis.JudgelsAPIForbiddenException;
import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.BundleProblemStatementRenderRequestV1;
import org.iatoki.judgels.sandalphon.controllers.api.util.TOTPUtils;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.statement.bundleStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public final class ClientBundleProblemStatementAPIControllerV1 extends AbstractJudgelsAPIController {

    private final ClientService clientService;
    private final ProblemService problemService;
    private final BundleItemService bundleItemService;

    @Inject
    public ClientBundleProblemStatementAPIControllerV1(ClientService clientService, ProblemService problemService, BundleItemService bundleItemService) {
        this.clientService = clientService;
        this.problemService = problemService;
        this.bundleItemService = bundleItemService;
    }

    @Transactional(readOnly = true)
    public Result renderStatement(String problemJid) throws IOException {
        BundleProblemStatementRenderRequestV1 requestBody = parseRequestBodyAsUrlFormEncoded(BundleProblemStatementRenderRequestV1.class);

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

            List<BundleItem> bundleItemList = bundleItemService.getBundleItemsInProblemWithClone(problem.getJid(), IdentityUtils.getUserJid());
            ImmutableList.Builder<Html> htmlBuilder = ImmutableList.builder();
            for (BundleItem bundleItem : bundleItemList) {
                BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
                htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfInProblemWithCloneByJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), statementLanguage)));
            }

            Html statementHtml = bundleStatementView.render(requestBody.postSubmitUrl, statement, htmlBuilder.build(), requestBody.reasonNotAllowedToSubmit);
            if (requestBody.switchStatementLanguageUrl != null) {
                statementHtml = statementLanguageSelectionLayout.render(requestBody.switchStatementLanguageUrl, allowedStatementLanguages, statementLanguage, statementHtml);
            }

            return ok(statementHtml);

        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }
}
