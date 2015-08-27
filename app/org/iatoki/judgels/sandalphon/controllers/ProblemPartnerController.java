package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.heading3WithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.partner.listPartnersView;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public class ProblemPartnerController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;

    @Inject
    public ProblemPartnerController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result viewPartners(long problemId) throws ProblemNotFoundException {
        return listPartners(problemId, 0, "id", "desc");
    }

    @Transactional(readOnly = true)
    public Result listPartners(long problemId, long pageIndex, String orderBy, String orderDir) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        Page<ProblemPartner> pageOfProblemPartners = problemService.getPageOfProblemPartners(problem.getJid(), pageIndex, PAGE_SIZE, orderBy, orderDir);

        LazyHtml content = new LazyHtml(listPartnersView.render(problem.getId(), pageOfProblemPartners, orderBy, orderDir));
        content.appendLayout(c -> heading3WithActionLayout.render(Messages.get("problem.partner.list"), new InternalLink(Messages.get("problem.partner.add"), routes.ProblemPartnerController.addPartner(problem.getId())), c));
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.list"), routes.ProblemPartnerController.viewPartners(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Partners");

        SandalphonControllerUtils.getInstance().addActivityLog("Open all partners <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    public Result addPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        switch (problem.getType()) {
            case PROGRAMMING:
                return redirect(routes.ProgrammingProblemPartnerController.addPartner(problem.getId()));
            case BUNDLE:
                return redirect(routes.BundleProblemPartnerController.addPartner(problem.getId()));
            default:
                return badRequest();
        }
    }

    @Transactional(readOnly = true)
    public Result updatePartner(long problemId, long partnerId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        switch (problem.getType()) {
            case PROGRAMMING:
                return redirect(routes.ProgrammingProblemPartnerController.updatePartner(problem.getId(), partnerId));
            case BUNDLE:
                return redirect(routes.BundleProblemPartnerController.updatePartner(problem.getId(), partnerId));
            default:
                return badRequest();
        }
    }
}
