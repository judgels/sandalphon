package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.controllers.CrudController;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.Problem;
import play.data.DynamicForm;
import play.mvc.Result;
import play.twirl.api.Html;

import org.iatoki.judgels.commons.views.html.layouts.main;

public final class ProblemController extends CrudController<Problem, ProblemDao> {

    @Override
    protected Object getReverseClass() {
        return routes.ProblemController;
    }

    @Override
    protected Result renderInsideLayout(int statusCode, String title, DynamicForm data, Html html) {
        return ok(main.render(title, html));
    }
}
