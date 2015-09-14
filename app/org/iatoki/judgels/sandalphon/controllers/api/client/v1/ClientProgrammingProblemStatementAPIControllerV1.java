package org.iatoki.judgels.sandalphon.controllers.api.client.v1;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.play.apis.JudgelsAPIForbiddenException;
import org.iatoki.judgels.play.apis.JudgelsAPIInternalServerErrorException;
import org.iatoki.judgels.play.apis.JudgelsAPINotFoundException;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapterRegistry;
import org.iatoki.judgels.sandalphon.controllers.api.object.v1.ProgrammingProblemStatementRenderRequestV1;
import org.iatoki.judgels.sandalphon.controllers.api.util.TOTPUtils;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public final class ClientProgrammingProblemStatementAPIControllerV1 extends AbstractJudgelsAPIController {

    private final ClientService clientService;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ClientProgrammingProblemStatementAPIControllerV1(ClientService clientService, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.clientService = clientService;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    public Result renderStatement(String problemJid) {
        ProgrammingProblemStatementRenderRequestV1 requestBody = parseRequestBodyAsUrlFormEncoded(ProgrammingProblemStatementRenderRequestV1.class);

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

            Html statementHtml = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(gradingEngine).renderViewStatement(requestBody.postSubmitUrl, statement, config, gradingEngine, finalAllowedGradingLanguages, requestBody.reasonNotAllowedToSubmit);
            if (requestBody.switchStatementLanguageUrl != null) {
                statementHtml = statementLanguageSelectionLayout.render(requestBody.switchStatementLanguageUrl, allowedStatementLanguages, statementLanguage, statementHtml);
            }

            return ok(statementHtml);

        } catch (IOException e) {
            throw new JudgelsAPIInternalServerErrorException(e);
        }
    }
}
