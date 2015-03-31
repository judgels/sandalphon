package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.UserRole;
import org.iatoki.judgels.sandalphon.UserRoleService;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

@Transactional
public final class ApplicationController extends Controller {

    private UserRoleService userRoleService;

    public ApplicationController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public Result index() {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(routes.ProblemController.index());
        } else if (session().containsKey("username")) {
            String returnUri = routes.ProblemController.index().absoluteURL(request());
            return redirect(routes.ApplicationController.auth(returnUri));
        } else {
            String returnUri = routes.ProblemController.index().absoluteURL(request());
            return redirect(routes.ApplicationController.auth(returnUri));
        }
    }

    public Result auth(String returnUri) {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(routes.ProblemController.index());
        } else if (session().containsKey("username")) {
            return redirect(routes.ApplicationController.authRole(returnUri));
        } else {
            return redirect(org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.login(returnUri));
        }
    }

    public Result authRole(String returnUri) {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(routes.ProblemController.index());
        } else {
            String userRoleJid = IdentityUtils.getUserJid();
            if (userRoleService.existsByUserJid(userRoleJid)) {
                UserRole userRole = userRoleService.findUserRoleByUserJid(userRoleJid);
                SandalphonUtils.saveRoleInSession(userRole.getRoles());
                return redirect(returnUri);
            } else {
                userRoleService.createUserRole(userRoleJid, SandalphonUtils.getDefaultRole());
                SandalphonUtils.saveRoleInSession(SandalphonUtils.getDefaultRole());
                return redirect(returnUri);
            }
        }
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
