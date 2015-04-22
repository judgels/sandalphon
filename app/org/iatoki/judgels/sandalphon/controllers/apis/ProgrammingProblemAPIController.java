package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.gabriel.commons.SubmissionAdapters;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.programming.GraderService;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public final class ProgrammingProblemAPIController extends Controller {
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;
    private final ClientService clientService;
    private final GraderService graderService;

    public ProgrammingProblemAPIController(ProblemService problemService, ProgrammingProblemService programmingProblemService, ClientService clientService, GraderService graderService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
        this.clientService = clientService;
        this.graderService = graderService;
    }

    public Result downloadGradingFiles() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        try {
            ByteArrayOutputStream os = programmingProblemService.getZippedGradingFilesStream(problemJid);
            response().setContentType("application/x-download");
            response().setHeader("Content-disposition", "attachment; filename=" + problemJid + ".zip");
            return ok(os.toByteArray()).as("application/zip");
        } catch (IOException e) {
            return internalServerError();
        }
    }

    public Result getGradingLastUpdateTime() {
        DynamicForm form = DynamicForm.form().bindFromRequest();

        String graderJid = form.get("graderJid");
        String graderSecret = form.get("graderSecret");
        String problemJid = form.get("problemJid");

        if (!problemService.problemExistsByJid(problemJid) || !graderService.verifyGrader(graderJid, graderSecret)) {
            return forbidden();
        }

        try {
            Date gradingLastUpdateTime = programmingProblemService.getGradingLastUpdateTime(null, problemJid);

            return ok("" + gradingLastUpdateTime.getTime());
        } catch (IOException e) {
            return internalServerError();
        }
    }


    public Result viewProblemStatementTOTP(String clientJid, String problemJid, int TOTP, String lang, String postSubmitUri, String switchLanguageUri) {
        response().setHeader("Access-Control-Allow-Origin", "*");
        if (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid)) {
            return notFound();
        }

        LanguageRestriction languageRestriction = new Gson().fromJson(request().body().asText(), LanguageRestriction.class);

        if (languageRestriction == null) {
            return badRequest();
        }

        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        if (!googleAuthenticator.authorize(new Base32().encodeAsString(clientProblem.getSecret().getBytes()), TOTP)) {
            return forbidden();
        }

        Problem problem = problemService.findProblemByJid(problemJid);
        String engine;
        try {
            engine = programmingProblemService.getGradingEngine(null, problem.getJid());
        } catch (IOException e) {
            engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }
        LanguageRestriction problemLanguageRestriction;
        try {
            problemLanguageRestriction = programmingProblemService.getLanguageRestriction(null, problem.getJid());
        } catch (IOException e) {
            problemLanguageRestriction = LanguageRestriction.defaultRestriction();
        }
        Set<String> allowedGradingLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(problemLanguageRestriction, languageRestriction));

        GradingConfig config;
        try {
            config = programmingProblemService.getGradingConfig(null, problem.getJid());
        } catch (IOException e) {
            config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
        }

        try {
            Map<String, StatementLanguageStatus> availableStatementLanguages = problemService.getAvailableLanguages(null, problem.getJid());

            if (!availableStatementLanguages.containsKey(lang) || availableStatementLanguages.get(lang) == StatementLanguageStatus.DISABLED) {
                lang = problemService.getDefaultLanguage(null, problemJid);
            }

            String statement = problemService.getStatement(null, problemJid, lang);

            Set<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet());

            Html html = SubmissionAdapters.fromGradingEngine(engine).renderViewStatement(postSubmitUri, problem.getName(), statement, config, engine, allowedGradingLanguageNames, null);
            html = SubmissionAdapters.fromGradingEngine(engine).renderStatementLanguageSelection(switchLanguageUri, allowedStatementLanguages, lang, html);
            return ok(html);
        } catch (IOException e) {
            return notFound();
        }
    }


}
