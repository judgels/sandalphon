package org.iatoki.judgels.sandalphon.controllers.security;

import org.iatoki.judgels.sandalphon.SandalphonUtils;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

public class HasRole extends Security.Authenticator {

    @Override
    public String getUsername(Http.Context context) {
        return SandalphonUtils.getRolesFromSession();
    }

    @Override
    public Result onUnauthorized(Http.Context context) {
        return redirect(org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.authRole(context.request().uri()));
    }
}
