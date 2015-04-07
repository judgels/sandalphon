package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.views.html.layouts.heading3WithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.problem.partner.listPartnersView;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public class ProblemPartnerController extends Controller {
    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;

    public ProblemPartnerController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result viewPartners(long problemId) {
        return listPartners(problemId, 0, "id", "desc");
    }

    public Result listPartners(long problemId, long pageIndex, String orderBy, String orderDir) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            Page<ProblemPartner> partners = problemService.pageProblemPartners(problem.getJid(), pageIndex, PAGE_SIZE, orderBy, orderDir);

            LazyHtml content = new LazyHtml(listPartnersView.render(problem.getId(), partners, orderBy, orderDir));
            content.appendLayout(c -> heading3WithActionLayout.render(Messages.get("problem.partner.list"), new InternalLink(Messages.get("problem.partner.add"), routes.ProblemPartnerController.addPartner(problem.getId())), c));
            ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.list"), routes.ProblemPartnerController.viewPartners(problem.getId())));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Partners");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    public Result addPartner(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            if (problem.getType() == ProblemType.PROGRAMMING) {
                return redirect(routes.ProgrammingProblemPartnerController.addPartner(problem.getId()));
            } else {
                return badRequest();
            }
        } else {
            return notFound();
        }
    }

    public Result updatePartner(long problemId, long partnerId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            if (problem.getType() == ProblemType.PROGRAMMING) {
                return redirect(routes.ProgrammingProblemPartnerController.updatePartner(problem.getId(), partnerId));
            } else {
                return badRequest();
            }
        } else {
            return notFound();
        }
    }
}
