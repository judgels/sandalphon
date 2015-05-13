package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ViewpointForm;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.jophiel.commons.JophielUtils;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.UserService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

@Transactional
public final class ApplicationController extends BaseController {

    private final UserService userService;

    public ApplicationController(UserService userService) {
        this.userService = userService;
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
            returnUri = org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterLogin(returnUri).absoluteURL(request(), request().secure());
            return redirect(org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.login(returnUri));
        }
    }

    public Result authRole(String returnUri) {
        if ((session().containsKey("username")) && (session().containsKey("role"))) {
            return redirect(returnUri);
        } else {
            String userRoleJid = IdentityUtils.getUserJid();
            if (userService.existsByUserJid(userRoleJid)) {
                User userRole = userService.findUserByUserJid(userRoleJid);
                SandalphonUtils.saveRolesInSession(userRole.getRoles());
                return redirect(returnUri);
            } else {
                userService.createUser(userRoleJid, SandalphonUtils.getDefaultRoles());
                SandalphonUtils.saveRolesInSession(SandalphonUtils.getDefaultRoles());
                return redirect(returnUri);
            }
        }
    }

    public Result afterLogin(String returnUri) {
        if (session().containsKey("role")) {
            JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());

            if (JudgelsUtils.hasViewPoint()) {
                try {
                    SandalphonUtils.backupSession();
                    SandalphonUtils.setUserSession(JophielUtils.getUserByUserJid(JudgelsUtils.getViewPoint()), userService.findUserByUserJid(JudgelsUtils.getViewPoint()));
                } catch (IOException e) {
                    JudgelsUtils.removeViewPoint();
                    SandalphonUtils.restoreSession();
                }
            }
            return redirect(returnUri);
        } else {
            returnUri = org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterLogin(returnUri).absoluteURL(request(), request().secure());
            return redirect(routes.ApplicationController.authRole(returnUri));
        }
    }

    public Result afterProfile(String returnUri) {
        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
        return redirect(returnUri);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result postViewAs() {
        Form<ViewpointForm> form = Form.form(ViewpointForm.class).bindFromRequest();

        if ((!(form.hasErrors() || form.hasGlobalErrors())) && (SandalphonUtils.trullyHasRole("admin"))) {
            ViewpointForm viewpointForm = form.get();
            String userJid = JophielUtils.verifyUsername(viewpointForm.username);
            if (userJid != null) {
                try {
                    userService.upsertUserFromJophielUserJid(userJid);
                    if (!JudgelsUtils.hasViewPoint()) {
                        SandalphonUtils.backupSession();
                    }
                    JudgelsUtils.setViewPointInSession(userJid);
                    SandalphonUtils.setUserSession(JophielUtils.getUserByUserJid(userJid), userService.findUserByUserJid(userJid));

                    ControllerUtils.getInstance().addActivityLog("View as user " + viewpointForm.username + ".");

                } catch (IOException e) {
                    JudgelsUtils.removeViewPoint();
                    SandalphonUtils.restoreSession();
                }
            }
        }
        return redirect(request().getHeader("Referer"));
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    public Result resetViewAs() {
        JudgelsUtils.removeViewPoint();
        SandalphonUtils.restoreSession();

        return redirect(request().getHeader("Referer"));
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
