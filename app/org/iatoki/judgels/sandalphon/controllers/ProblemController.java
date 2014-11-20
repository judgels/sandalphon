package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.controllers.CrudController;
import org.iatoki.judgels.sandalphon.models.dao.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.schema.Problem;

public final class ProblemController extends CrudController<Problem, ProblemDao> {

    @Override
    protected Object getReverseClass() {
        return routes.ProblemController;
    }

}
