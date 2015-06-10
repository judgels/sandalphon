package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FilenameUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.ClientService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemService;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemService;
import org.iatoki.judgels.sandalphon.commons.SubmissionAdapters;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.commons.views.html.bundleStatementView;
import org.iatoki.judgels.sandalphon.commons.views.html.statementLanguageSelectionLayout;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import play.twirl.api.Html;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public final class ProblemAPIController extends Controller {
    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;
    private final BundleItemService bundleItemService;
    private final ProgrammingProblemService programmingProblemService;
    private final ClientService clientService;

    public ProblemAPIController(ProblemService problemService, BundleProblemService bundleProblemService, BundleItemService bundleItemService, ProgrammingProblemService programmingProblemService, ClientService clientService) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
        this.bundleItemService = bundleItemService;
        this.programmingProblemService = programmingProblemService;
        this.clientService = clientService;
    }

    public Result renderMediaById(long problemId, String imageFilename) throws ProblemNotFoundException {
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
        if (clientService.clientExistsByClientJid(clientJid)) {
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

    public Result viewProblemStatementTOTP() {
        response().setHeader("Access-Control-Allow-Origin", "*");

        DynamicForm form = DynamicForm.form().bindFromRequest();
        String clientJid = form.get("clientJid");
        String problemJid = form.get("problemJid");
        int tOTP = 0;
        if (form.get("TOTP") != null) {
            tOTP = Integer.parseInt(form.get("TOTP"));
        }
        String lang = form.get("lang");
        String postSubmitUri = form.get("postSubmitUri");
        String switchLanguageUri = form.get("switchLanguageUri");
        String reasonNotAllowedToSubmit = form.get("reasonNotAllowedToSubmit");

        if ((!clientService.clientExistsByClientJid(clientJid)) && (!problemService.problemExistsByJid(problemJid)) && (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid))) {
            return notFound();
        }

        Problem problem = problemService.findProblemByJid(problemJid);
        ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        if (!googleAuthenticator.authorize(new Base32().encodeAsString(clientProblem.getSecret().getBytes()), tOTP)) {
            return forbidden();
        }

        try {
            Map<String, StatementLanguageStatus> availableStatementLanguages = problemService.getAvailableLanguages(null, problem.getJid());

            if (!availableStatementLanguages.containsKey(lang) || availableStatementLanguages.get(lang) == StatementLanguageStatus.DISABLED) {
                lang = problemService.getDefaultLanguage(null, problemJid);
            }

            String statement = problemService.getStatement(null, problemJid, lang);

            Set<String> allowedStatementLanguages = availableStatementLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet());

            if (problem.getType().equals(ProblemType.PROGRAMMING)) {
                LanguageRestriction languageRestriction = new Gson().fromJson(form.get("languageRestriction"), LanguageRestriction.class);
                return processProgrammingProblem(problem, statement, allowedStatementLanguages, lang, postSubmitUri, switchLanguageUri, languageRestriction, reasonNotAllowedToSubmit);
            } else if (problem.getType().equals(ProblemType.BUNDLE)) {
                return processBundleProblem(problem, statement, allowedStatementLanguages, lang, postSubmitUri, switchLanguageUri, reasonNotAllowedToSubmit);
            } else {
                return notFound();
            }
        } catch (IOException e) {
            return notFound();
        }
    }

    private Result processProgrammingProblem(Problem problem, String statement, Set<String> allowedStatementLanguages, String lang, String postSubmitUri, String switchLanguageUri, LanguageRestriction languageRestriction, String reasonNotAllowedToSubmit) {
        if (languageRestriction == null) {
            return badRequest();
        }

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

        Html html = SubmissionAdapters.fromGradingEngine(engine).renderViewStatement(postSubmitUri, problem.getName(), statement, config, engine, allowedGradingLanguageNames, reasonNotAllowedToSubmit);
        if (switchLanguageUri != null) {
            html = SubmissionAdapters.fromGradingEngine(engine).renderStatementLanguageSelection(switchLanguageUri, allowedStatementLanguages, lang, html);
        }
        return ok(html);
    }

    private Result processBundleProblem(Problem problem, String statement, Set<String> allowedStatementLanguages, String lang, String postSubmitUri, String switchLanguageUri, String reasonNotAllowedToSubmit) throws IOException {
        List<BundleItem> bundleItemList = bundleItemService.findAllItems(problem.getJid(), IdentityUtils.getUserJid());
        ImmutableList.Builder<Html> htmlBuilder = ImmutableList.builder();
        for (BundleItem bundleItem : bundleItemList) {
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfByItemJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), lang)));
        }

        String language = lang;

        Html html = bundleStatementView.render(postSubmitUri, problem.getName(), statement, htmlBuilder.build(), reasonNotAllowedToSubmit);
        LazyHtml content = new LazyHtml(html);
        if (switchLanguageUri != null) {
            content.appendLayout(c -> statementLanguageSelectionLayout.render(switchLanguageUri, allowedStatementLanguages, language, c));
        }

        return Results.ok(content.render());
    }
}
