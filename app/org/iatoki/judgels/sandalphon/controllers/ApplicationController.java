package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.jophiel.commons.JophielUtils;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.UserService;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Transactional
public final class ApplicationController extends BaseController {

    private final UserService userRoleService;

    public ApplicationController(UserService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public Result index() {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(routes.ProblemController.index());
        } else if (session().containsKey("username")) {
            String returnUri = routes.ProblemController.index().absoluteURL(request(), request().secure());
            return redirect(routes.ApplicationController.authRole(returnUri));
        } else {
            String returnUri = routes.ProblemController.index().absoluteURL(request(), request().secure());
            return redirect(routes.ApplicationController.auth(returnUri));
        }
    }

    public Result auth(String returnUri) {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(returnUri);
        } else if (session().containsKey("username")) {
            return redirect(routes.ApplicationController.authRole(returnUri));
        } else {
            returnUri = org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterLogin(routes.ApplicationController.authRole(returnUri).absoluteURL(request(), request().secure())).absoluteURL(request(), request().secure());
            return redirect(org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.login(returnUri));
        }
    }

    public Result authRole(String returnUri) {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(returnUri);
        } else {
            String userRoleJid = IdentityUtils.getUserJid();
            if (userRoleService.existsByUserJid(userRoleJid)) {
                User userRole = userRoleService.findUserByUserJid(userRoleJid);
                SandalphonUtils.saveRoleInSession(userRole.getRoles());
                return redirect(returnUri);
            } else {
                userRoleService.createUser(userRoleJid, SandalphonUtils.getDefaultRole());
                SandalphonUtils.saveRoleInSession(SandalphonUtils.getDefaultRole());
                return redirect(returnUri);
            }
        }
    }

    public Result afterLogin(String returnUri) {
        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
        return redirect(returnUri);
    }

    public Result afterProfile(String returnUri) {
        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
        return redirect(returnUri);
    }

    private Result getResult(LazyHtml content, int statusCode) {
        switch (statusCode) {
            case Http.Status.OK:
                return ok(content.render(0));
            case Http.Status.NOT_FOUND:
                return notFound(content.render(0));
            default:
                return badRequest(content.render(0));
        }
    }
}
