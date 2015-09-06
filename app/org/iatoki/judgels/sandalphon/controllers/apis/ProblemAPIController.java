package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.sandalphon.ResourceDisplayName;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapterRegistry;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.statement.bundleStatementView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;
import play.twirl.api.Html;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
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

@Singleton
@Named
public final class ProblemAPIController extends Controller {

    private final BundleItemService bundleItemService;
    private final ClientService clientService;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ProblemAPIController(BundleItemService bundleItemService, ClientService clientService, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.bundleItemService = bundleItemService;
        this.clientService = clientService;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Result verifyProblem() {
        UsernamePasswordCredentials credentials = JudgelsPlayUtils.parseBasicAuthFromRequest(request());

        if (credentials == null) {
            response().setHeader("WWW-Authenticate", "Basic realm=\"" + request().host() + "\"");
            return unauthorized();
        }

        String clientJid = credentials.getUserName();
        String clientSecret = credentials.getPassword();

        if (!clientService.clientExistsByJid(clientJid)) {
            return notFound();
        }

        Client client = clientService.findClientByJid(clientJid);
        if (!client.getSecret().equals(clientSecret)) {
            return forbidden();
        }

        DynamicForm form = DynamicForm.form().bindFromRequest();

        String problemJid = form.get("problemJid");
        if (!problemService.problemExistsByJid(problemJid)) {
            return notFound();
        }

        try {
            Problem problem = problemService.findProblemByJid(problemJid);

            ResourceDisplayName displayName = new ResourceDisplayName();
            displayName.defaultLanguage = problemService.getDefaultLanguage(null, problemJid);
            displayName.titlesByLanguage = problemService.getTitlesByLanguage(null, problemJid);
            displayName.slug = problem.getSlug();

            return ok(new Gson().toJson(displayName));
        } catch (IOException e) {
            return internalServerError();
        }
    }

    @Transactional(readOnly = true)
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

        if ((!clientService.clientExistsByJid(clientJid)) && (!problemService.problemExistsByJid(problemJid)) && (!clientService.isClientAuthorizedForProblem(problemJid, clientJid))) {
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

            ProblemStatement statement = problemService.getStatement(null, problemJid, lang);

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

    private Result processProgrammingProblem(Problem problem, ProblemStatement statement, Set<String> allowedStatementLanguages, String lang, String postSubmitUri, String switchLanguageUri, LanguageRestriction languageRestriction, String reasonNotAllowedToSubmit) {
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

        Html html = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine).renderViewStatement(postSubmitUri, statement, config, engine, allowedGradingLanguageNames, reasonNotAllowedToSubmit);
        if (switchLanguageUri != null) {
            html = statementLanguageSelectionLayout.render(switchLanguageUri, allowedStatementLanguages, lang, html);
        }
        return ok(html);
    }

    private Result processBundleProblem(Problem problem, ProblemStatement statement, Set<String> allowedStatementLanguages, String lang, String postSubmitUri, String switchLanguageUri, String reasonNotAllowedToSubmit) throws IOException {
        List<BundleItem> bundleItemList = bundleItemService.getBundleItemsInProblemWithClone(problem.getJid(), IdentityUtils.getUserJid());
        ImmutableList.Builder<Html> htmlBuilder = ImmutableList.builder();
        for (BundleItem bundleItem : bundleItemList) {
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfInProblemWithCloneByJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), lang)));
        }

        String language = lang;

        Html html = bundleStatementView.render(postSubmitUri, statement, htmlBuilder.build(), reasonNotAllowedToSubmit);
        LazyHtml content = new LazyHtml(html);
        if (switchLanguageUri != null) {
            content.appendLayout(c -> statementLanguageSelectionLayout.render(switchLanguageUri, allowedStatementLanguages, language, c));
        }

        return Results.ok(content.render());
    }
}
