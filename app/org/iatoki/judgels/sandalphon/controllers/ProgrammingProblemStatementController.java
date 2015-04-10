package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.commons.SubmissionAdapters;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemStatementController extends Controller {
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProgrammingProblemStatementController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    public Result viewStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProblemControllerUtils.establishStatementLanguage(problemService, problem);

        if (ProblemControllerUtils.isAllowedToViewStatement(problemService, problem)) {
            String statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());

            GradingConfig config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
            String engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
            LanguageRestriction languageRestriction = programmingProblemService.getLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid());
            Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

            boolean isAllowedToSubmitByPartner = ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem);
            boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

            String reasonNotAllowedToSubmit = null;

            if (!isAllowedToSubmitByPartner) {
                reasonNotAllowedToSubmit = Messages.get("problem.programming.cantSubmit");
            } else if (!isClean) {
                reasonNotAllowedToSubmit = Messages.get("problem.programming.cantSubmitNotClean");
            }

            LazyHtml content = new LazyHtml(SubmissionAdapters.fromGradingEngine(engine).renderViewStatement(routes.ProgrammingProblemSubmissionController.postSubmit(problemId).absoluteURL(request(), request().secure()), problem.getName(), statement, config, engine, allowedLanguageNames, reasonNotAllowedToSubmit));

            Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToView(problemService, problem);

            ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemStatementController.viewStatementSwitchLanguage(problem.getId()));

            ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
            ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.view"), routes.ProblemStatementController.viewStatement(problemId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

            ControllerUtils.getInstance().addActivityLog("View statement of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }
}
