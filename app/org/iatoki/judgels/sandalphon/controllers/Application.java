package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.sandalphon.views.html.application.index;
import play.mvc.Controller;
import play.mvc.Result;

public final class Application extends Controller {

    public static Result index() {
        return ok(index.render("Your new application is ready."));
    }

}
