package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.helpers.WrappedContents;
import org.iatoki.judgels.commons.controllers.crud.BasicCrudController;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarView;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.Problem;
import play.twirl.api.Html;

public final class ProblemController extends BasicCrudController<Problem, ProblemDao> {

    @Override
    protected WrappedContents wrapWithTemplate(WrappedContents content) {
        ImmutableList.Builder<Html> widgets = ImmutableList.builder();
        widgets.add(Html.apply("Coba"));

        return content.wrapWithTransformation(c -> leftSidebarView.render(getPageTitle(), c, widgets.build()));
    }

    @Override
    protected Object getReverseController() {
        return routes.ProblemController;
    }
}
