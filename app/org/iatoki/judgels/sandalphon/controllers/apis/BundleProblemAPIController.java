package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.gson.Gson;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleGradingResult;
import org.iatoki.judgels.sandalphon.services.impls.BundleProblemGraderImpl;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
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

    private final ProblemService problemService;
    private final ClientService clientService;
    private final BundleProblemGraderImpl bundleProblemGrader;

    @Inject
    public BundleProblemAPIController(ProblemService problemService, ClientService clientService, BundleProblemGraderImpl bundleProblemGrader) {
        this.problemService = problemService;
        this.clientService = clientService;
        this.bundleProblemGrader = bundleProblemGrader;
    }

    @Transactional(readOnly = true)
    public Result gradeProblem() {
        UsernamePasswordCredentials credentials = JudgelsPlayUtils.parseBasicAuthFromRequest(request());

        if (credentials != null) {
            DynamicForm form = DynamicForm.form().bindFromRequest();

            String clientJid = credentials.getUserName();
            String clientSecret = credentials.getPassword();
            String problemJid = form.get("problemJid");

            try {
                response().setHeader("Access-Control-Allow-Origin", "*");
                if ((!clientService.clientExistsByClientJid(clientJid)) && (!problemService.problemExistsByJid(problemJid)) && (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid))) {
                    return notFound();
                }

                Client client = clientService.findClientByJid(clientJid);
                Problem problem = problemService.findProblemByJid(problemJid);
                ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

                if (!client.getSecret().equals(clientSecret)) {
                    return forbidden();
                }

                BundleAnswer bundleAnswer = new Gson().fromJson(form.get("answer"), BundleAnswer.class);
                BundleGradingResult gradingResult = bundleProblemGrader.gradeBundleProblem(problem.getJid(), bundleAnswer);

                return ok(new Gson().toJson(gradingResult));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            response().setHeader("WWW-Authenticate", "Basic realm=\"" + request().host() + "\"");
            return unauthorized();
        }
    }

}
