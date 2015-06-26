package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.forms.programming.GradingEngineUpdateForm;
import org.iatoki.judgels.sandalphon.forms.programming.LanguageRestrictionUpdateForm;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapter;
import org.iatoki.judgels.sandalphon.programming.GradingConfigAdapters;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithAutoPopulation;
import org.iatoki.judgels.sandalphon.programming.adapters.ConfigurableWithTokilibFormat;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.autoPopulationLayout;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.tokilibLayout;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.listGradingHelperFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.listGradingTestDataFilesView;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.updateGradingEngineView;
import org.iatoki.judgels.sandalphon.views.html.programming.grading.updateLanguageRestrictionView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemGradingController extends BaseController {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProgrammingProblemGradingController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateGradingEngine(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            GradingEngineUpdateForm data = new GradingEngineUpdateForm();
            try  {
                data.gradingEngineName = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch(IOException e) {
                data.gradingEngineName = GradingEngineRegistry.getInstance().getDefaultEngine();
            }

            Form<GradingEngineUpdateForm> form = Form.form(GradingEngineUpdateForm.class).fill(data);

            ControllerUtils.getInstance().addActivityLog("Try to update grading engine of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateGradingEngine(form, problem);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateGradingEngine(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Form<GradingEngineUpdateForm> form = Form.form(GradingEngineUpdateForm.class).bindFromRequest(request());

            if (form.hasErrors() || form.hasGlobalErrors()) {
                return showUpdateGradingEngine(form, problem);
            } else {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                String gradingEngine = form.get().gradingEngineName;
                String originalGradingEngine;
                try {
                    originalGradingEngine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
                } catch (IOException e) {
                    originalGradingEngine = GradingEngineRegistry.getInstance().getDefaultEngine();
                }

                try {
                    if (!gradingEngine.equals(originalGradingEngine)) {
                        GradingConfig config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
                        programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), config);
                    }

                    programmingProblemService.updateGradingEngine(IdentityUtils.getUserJid(), problem.getJid(), gradingEngine);

                    ControllerUtils.getInstance().addActivityLog("Update grading engine of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId()));
                } catch (IOException e) {
                    form.reject("problem.programming.grading.engine.error.cantUpdate");
                    return showUpdateGradingEngine(form, problem);
                }
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateGradingConfig(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }
            GradingConfig config;
            try {
                config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
            }
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
            List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

            Form<?> form = GradingConfigAdapters.fromGradingType(engine).createFormFromConfig(config);

            ControllerUtils.getInstance().addActivityLog("Try to update grading config of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateGradingConfig(form, problem, engine, testDataFiles, helperFiles);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateGradingConfig(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        
        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }
            Form<?> form = GradingConfigAdapters.fromGradingType(engine).createEmptyForm().bindFromRequest(request());

            if (form.hasErrors() || form.hasGlobalErrors()) {
                List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

                return showUpdateGradingConfig(form, problem, engine, testDataFiles, helperFiles);
            } else {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                try {
                    GradingConfig config = GradingConfigAdapters.fromGradingType(engine).createConfigFromForm(form);
                    programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), config);

                    ControllerUtils.getInstance().addActivityLog("Update grading config of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId()));
                } catch (IOException e) {
                    form.reject("problem.programming.grading.config.error.cantUpdate");
                    List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                    List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

                    return showUpdateGradingConfig(form, problem, engine, testDataFiles, helperFiles);
                }
            }
        } else {
            return notFound();
        }
    }

    @Transactional
    public Result updateGradingConfigByTokilibFormat(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }

            GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(engine);

            if (!(adapter instanceof ConfigurableWithTokilibFormat)) {
                return forbidden();
            }

            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
            GradingConfig config;
            try {
                config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
            }

            try {
                GradingConfig newConfig = ((ConfigurableWithTokilibFormat) adapter).updateConfigWithTokilibFormat(config, testDataFiles);
                programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), newConfig);

                ControllerUtils.getInstance().addActivityLog("Update grading config using tokilib format of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId()));
            } catch (IOException e) {
                throw new IllegalStateException("Can't update grading config using tokilib format", e);
            }
        } else {
            return notFound();
        }
    }

    @Transactional
    public Result updateGradingConfigByAutoPopulation(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            String engine;
            try {
                engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                engine = GradingEngineRegistry.getInstance().getDefaultEngine();
            }
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
            GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(engine);

            if (!(adapter instanceof ConfigurableWithAutoPopulation)) {
                return forbidden();
            }

            GradingConfig config;
            try {
                config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
            }
            GradingConfig newConfig = ((ConfigurableWithAutoPopulation) adapter).updateConfigWithAutoPopulation(config, testDataFiles);

            try {
                programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), newConfig);

                ControllerUtils.getInstance().addActivityLog("Update grading config by auto population of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return redirect(routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId()));
            } catch (IOException e) {
                throw new IllegalStateException("Can't update grading config using auto population", e);
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result listGradingTestDataFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Form<UploadFileForm> form = Form.form(UploadFileForm.class);
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());

            ControllerUtils.getInstance().addActivityLog("List grading test data files of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showListGradingTestDataFiles(form, problem, testDataFiles);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUploadGradingTestDataFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart file;

            file = body.getFile("file");
            if (file != null) {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                File testDataFile = file.getFile();
                try {
                    programmingProblemService.uploadGradingTestDataFile(IdentityUtils.getUserJid(), problem.getJid(), testDataFile, file.getFilename());

                    ControllerUtils.getInstance().addActivityLog("Upload test data file of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
                } catch (IOException e){
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                    form.reject("problem.programming.grading.error.cantUploadTestData");

                    return showListGradingTestDataFiles(form, problem, testDataFiles);
                }
            }

            file = body.getFile("fileZipped");
            if (file != null) {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                File testDataFile = file.getFile();
                try {
                    programmingProblemService.uploadGradingTestDataFileZipped(IdentityUtils.getUserJid(), problem.getJid(), testDataFile);

                    ControllerUtils.getInstance().addActivityLog("Upload zipped test data files of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
                } catch (IOException e) {
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                    form.reject("problem.programming.grading.error.cantUploadTestDataZipped");

                    return showListGradingTestDataFiles(form, problem, testDataFiles);
                }
            }

            return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result listGradingHelperFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Form<UploadFileForm> form = Form.form(UploadFileForm.class);
            List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

            ControllerUtils.getInstance().addActivityLog("List grading helper files of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showListGradingHelperFiles(form, problem, helperFiles);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUploadGradingHelperFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart file;

            file = body.getFile("file");
            if (file != null) {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                File helperFile = file.getFile();
                try {
                    programmingProblemService.uploadGradingHelperFile(IdentityUtils.getUserJid(), problem.getJid(), helperFile, file.getFilename());

                    ControllerUtils.getInstance().addActivityLog("Upload helper file of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
                } catch (IOException e) {
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());
                    form.reject("problem.programming.grading.error.cantUploadHelper");

                    return showListGradingHelperFiles(form, problem, helperFiles);
                }
            }

            file = body.getFile("fileZipped");
            if (file != null) {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                File helperFile = file.getFile();
                try {
                    programmingProblemService.uploadGradingHelperFileZipped(IdentityUtils.getUserJid(), problem.getJid(), helperFile);

                    ControllerUtils.getInstance().addActivityLog("Upload zipped helper files of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
                } catch (IOException e) {
                    Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                    List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());
                    form.reject("problem.programming.grading.error.cantUploadHelperZipped");

                    return showListGradingHelperFiles(form, problem, helperFiles);
                }
            }

            return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result downloadGradingTestDataFile(long id, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(id);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            String testDataURL = programmingProblemService.getGradingTestDataFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

            ControllerUtils.getInstance().addActivityLog("Download test data file " + filename + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            try {
                new URL(testDataURL);
                return redirect(testDataURL);
            } catch (MalformedURLException e) {
                File testDataFile = new File(testDataURL);
                return ProblemControllerUtils.downloadFile(testDataFile);
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result downloadGradingHelperFile(long id, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(id);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            String helperURL = programmingProblemService.getGradingHelperFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

            ControllerUtils.getInstance().addActivityLog("Download helper file " + filename + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            try {
                new URL(helperURL);
                return redirect(helperURL);
            } catch (MalformedURLException e) {
                File helperFile = new File(helperURL);
                return ProblemControllerUtils.downloadFile(helperFile);
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateLanguageRestriction(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            LanguageRestriction languageRestriction;
            try {
                languageRestriction = programmingProblemService.getLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid());
            } catch (IOException e) {
                languageRestriction = LanguageRestriction.defaultRestriction();
            }

            LanguageRestrictionUpdateForm data = new LanguageRestrictionUpdateForm();
            data.allowedLanguageNames = LanguageRestrictionAdapter.getFormAllowedLanguageNamesFromLanguageRestriction(languageRestriction);
            data.isAllowedAll = LanguageRestrictionAdapter.getFormIsAllowedAllFromLanguageRestriction(languageRestriction);

            Form<LanguageRestrictionUpdateForm> form = Form.form(LanguageRestrictionUpdateForm.class).fill(data);

            ControllerUtils.getInstance().addActivityLog("Try to update language restriction of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateLanguageRestriction(form, problem);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateLanguageRestriction(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            Form<LanguageRestrictionUpdateForm> form = Form.form(LanguageRestrictionUpdateForm.class).bindFromRequest(request());

            if (form.hasErrors() || form.hasGlobalErrors()) {
                return showUpdateLanguageRestriction(form, problem);
            } else {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                LanguageRestrictionUpdateForm data = form.get();
                LanguageRestriction languageRestriction = LanguageRestrictionAdapter.createLanguageRestrictionFromForm(data.allowedLanguageNames, data.isAllowedAll);

                try {
                    programmingProblemService.updateLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid(), languageRestriction);

                    ControllerUtils.getInstance().addActivityLog("Update language restriction of problem " + problem.getName() + ".");

                    return redirect(routes.ProgrammingProblemGradingController.updateLanguageRestriction(problem.getId()));
                } catch (IOException e) {
                    form.reject("problem.programming.language.error.cantUpdate");
                    return showUpdateLanguageRestriction(form, problem);
                }
            }
        } else {
            return notFound();
        }
    }

    private Result showUpdateGradingEngine(Form<GradingEngineUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateGradingEngineView.render(form, problem));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.engine.update"), routes.ProgrammingProblemGradingController.updateGradingEngine(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Grading Engine");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateGradingConfig(Form<?> form, Problem problem, String gradingEngine, List<FileInfo> testDataFiles, List<FileInfo> helperFiles) {
        GradingConfigAdapter adapter = GradingConfigAdapters.fromGradingType(gradingEngine);
        LazyHtml content = new LazyHtml(adapter.renderUpdateGradingConfig(form, problem, testDataFiles, helperFiles));

        if (adapter instanceof ConfigurableWithTokilibFormat) {
            content.appendLayout(c -> tokilibLayout.render(problem.getId(), c));
        } else if (adapter instanceof ConfigurableWithAutoPopulation) {
            content.appendLayout(c -> autoPopulationLayout.render(problem.getId(), c));
        }

        appendSubtabsLayout(content, problem);

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.config.update"), routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Grading Config");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingTestDataFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> testDataFiles) {
        LazyHtml content = new LazyHtml(listGradingTestDataFilesView.render(form, problem.getId(), testDataFiles));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.testData.list"), routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - List Grading Test Data Files");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingHelperFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> helperFiles) {
        LazyHtml content = new LazyHtml(listGradingHelperFilesView.render(form, problem.getId(), helperFiles));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.helper.list"), routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - List Grading Helper Files");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateLanguageRestriction(Form<LanguageRestrictionUpdateForm> form, Problem problem) {
        LazyHtml content = new LazyHtml(updateLanguageRestrictionView.render(form, problem));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.languageRestriction.update"), routes.ProgrammingProblemGradingController.updateLanguageRestriction(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Language Restriction");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Problem problem) {
        content.appendLayout(c -> accessTypesLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemGradingController.updateGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemGradingController.updateGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemGradingController.updateLanguageRestriction(problem.getId()))
        ), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
