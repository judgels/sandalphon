package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.controllers.crud.BasicCrudController;
import org.iatoki.judgels.commons.helpers.NamedCall;
import org.iatoki.judgels.commons.helpers.LazyHtml;
import org.iatoki.judgels.commons.helpers.TabbedSectionLayout;
import org.iatoki.judgels.commons.helpers.Utilities;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarView;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProgrammingProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingProblem;
import org.iatoki.judgels.sandalphon.models.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.programmingproblem.updateStatementView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import java.util.List;


public final class ProgrammingProblemController extends BasicCrudController<ProgrammingProblem, ProgrammingProblemDao> {

    public ProgrammingProblemController() {
        setUpdateLayout(new TabbedSectionLayout(ImmutableList.of(
                id -> new NamedCall("general", routes.ProgrammingProblemController.update(id)),
                id -> new NamedCall("statement", routes.ProgrammingProblemController.updateStatement(id))
        )));
    }

    @Transactional
    public Result updateStatement(long id) {
        ProgrammingProblem problem = getDao().findById(id);
        Form<ProgrammingProblem> form = Form.form(getModelClass()).fill(problem);
        return getResult(wrapUpdateContent(updateStatementView.render(form, routes.ProgrammingProblemController.doUpdateStatement(id)), problem.getHumanFriendlyName(), id), Http.Status.OK);
    }

    @Transactional
    public Result doUpdateStatement(long id) {
        ProgrammingProblem problem = Form.form(getModelClass()).bindFromRequest().get();
        ProgrammingProblemService.updateStatement(id, problem.statement, Utilities.getUserIdFromSession(session()), Utilities.getIpAddressFromRequest(request()));

        return redirect(routes.ProgrammingProblemController.view(id));
    }

    @Override
    protected void wrapWithTemplate(LazyHtml content, List<NamedCall> breadcrumbs) {
        ImmutableList.Builder<Html> widgets = ImmutableList.builder();
        widgets.add(Html.apply("Coba"));

        content.appendTransformation(c -> leftSidebarView.render(getPageTitle(), breadcrumbs, widgets.build(), c));
    }

    @Override
    protected Object getReverseController() {
        return routes.ProgrammingProblemController;
    }
}
