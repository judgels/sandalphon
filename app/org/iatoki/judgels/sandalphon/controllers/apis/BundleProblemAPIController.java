package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.gson.Gson;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleGradingResult;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.impls.BundleProblemGraderImpl;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Named
public final class BundleProblemAPIController extends Controller {

    private final BundleProblemGraderImpl bundleProblemGrader;
    private final ClientService clientService;
    private final ProblemService problemService;

    @Inject
    public BundleProblemAPIController(BundleProblemGraderImpl bundleProblemGrader, ClientService clientService, ProblemService problemService) {
        this.bundleProblemGrader = bundleProblemGrader;
        this.clientService = clientService;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result gradeProblem() {
        UsernamePasswordCredentials credentials = JudgelsPlayUtils.parseBasicAuthFromRequest(request());

        if (credentials == null) {
            response().setHeader("WWW-Authenticate", "Basic realm=\"" + request().host() + "\"");
            return unauthorized();
        }

        DynamicForm form = DynamicForm.form().bindFromRequest();

        String clientJid = credentials.getUserName();
        String clientSecret = credentials.getPassword();
        String problemJid = form.get("problemJid");

        response().setHeader("Access-Control-Allow-Origin", "*");
        if (!clientService.clientExistsByJid(clientJid) && !problemService.problemExistsByJid(problemJid) && !clientService.isClientAuthorizedForProblem(problemJid, clientJid)) {
            return notFound();
        }

        Client client = clientService.findClientByJid(clientJid);
        Problem problem = problemService.findProblemByJid(problemJid);
        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

        if (!client.getSecret().equals(clientSecret)) {
            return forbidden();
        }

        BundleAnswer bundleAnswer = new Gson().fromJson(form.get("answer"), BundleAnswer.class);
        BundleGradingResult bundleGradingResult;
        try {
            bundleGradingResult = bundleProblemGrader.gradeBundleProblem(problem.getJid(), bundleAnswer);
        } catch (IOException e) {
            return notFound();
        }

        return ok(new Gson().toJson(bundleGradingResult));
    }

}
