package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.helpers.WrappedContents;
import org.iatoki.judgels.commons.controllers.crud.BasicCrudController;
import org.iatoki.judgels.commons.views.html.layouts.standardView;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.Problem;

public final class ProblemController extends BasicCrudController<Problem, ProblemDao> {

    @Override
    protected WrappedContents wrapWithTemplate(WrappedContents content) {
        return content.wrapWithTransformation(c -> standardView.render(getPageTitle(), c));
    }

    @Override
    protected Object getReverseController() {
        return routes.ProblemController;
    }
}
