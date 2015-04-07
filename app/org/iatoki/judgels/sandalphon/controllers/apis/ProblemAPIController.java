package org.iatoki.judgels.sandalphon.controllers.apis;

import org.apache.commons.io.FilenameUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

@Transactional
public final class ProblemAPIController extends Controller {
    private final ProblemService problemService;
    private final ClientService clientService;

    public ProblemAPIController(ProblemService problemService, ClientService clientService) {
        this.problemService = problemService;
        this.clientService = clientService;
    }

    public Result renderMediaById(long problemId, String imageFilename) {
        Problem problem = problemService.findProblemById(problemId);
        String mediaURL = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), imageFilename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File image = new File(mediaURL);

            if (!image.exists()) {
                return notFound();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            response().setHeader("Cache-Control", "no-transform,public,max-age=300,s-maxage=900");
            response().setHeader("Last-Modified", sdf.format(new Date(image.lastModified())));

            try {
                BufferedImage in = ImageIO.read(image);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                String type = FilenameUtils.getExtension(image.getAbsolutePath());

                ImageIO.write(in, type, baos);
                return ok(baos.toByteArray()).as("image/" + type);
            } catch (IOException e2) {
                return internalServerError();
            }
        }
    }

    public Result renderMediaByJid(String problemJid, String imageFilename) {
        String mediaURL = problemService.getStatementMediaFileURL(null, problemJid, imageFilename);

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File image = new File(mediaURL);

            if (!image.exists()) {
                return notFound();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            response().setHeader("Cache-Control", "no-transform,public,max-age=300,s-maxage=900");
            response().setHeader("Last-Modified", sdf.format(new Date(image.lastModified())));

            try {
                BufferedImage in = ImageIO.read(image);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                String type = FilenameUtils.getExtension(image.getAbsolutePath());

                ImageIO.write(in, type, baos);
                return ok(baos.toByteArray()).as("image/" + type);
            } catch (IOException e2) {
                return internalServerError();
            }
        }
    }

    public Result verifyProblem() {
        DynamicForm form = DynamicForm.form().bindFromRequest();
        String clientJid = form.get("clientJid");
        String clientSecret = form.get("clientSecret");
        if (clientService.existsByJid(clientJid)) {
            Client client = clientService.findClientByJid(clientJid);
            if (client.getSecret().equals(clientSecret)) {
                String problemJid = form.get("problemJid");
                if (problemService.problemExistsByJid(problemJid)) {
                    return ok(problemService.findProblemByJid(problemJid).getName());
                } else {
                    return notFound();
                }
            } else {
                return forbidden();
            }
        } else {
            return notFound();
        }
    }
}
