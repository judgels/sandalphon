package org.iatoki.judgels.sandalphon.controllers.security;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public final class LoggedIn extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        return context.session().get("username");
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.auth(context.request().uri()));
    }
}
