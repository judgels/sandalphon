package org.iatoki.judgels.sandalphon.helpers.authenticators;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public final class AdminAuthenticator extends Security.Authenticator {

    @Override
    public String getUsername(Context ctx) {
        if ((ctx.session().get("access") != null) && ("admin".equals(ctx.session().get("access"))))  {
            return ctx.session().get("username");
        } else {
            return null;
        }
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        return forbidden();
    }

}
