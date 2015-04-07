package org.iatoki.judgels.sandalphon.controllers.security;

import org.iatoki.judgels.jophiel.commons.controllers.security.BaseLoggedIn;
import play.Play;
import play.mvc.Call;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public final class LoggedIn extends BaseLoggedIn {

    @Override
    public Call getRedirectCall(Http.Context context) {
        return org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.auth(context.request().uri());
    }

}
