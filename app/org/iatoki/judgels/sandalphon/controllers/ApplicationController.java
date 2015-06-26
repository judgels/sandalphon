package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.controllers.forms.ViewpointForm;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.JidCacheService;
import org.iatoki.judgels.sandalphon.services.UserService;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;

import java.io.IOException;

public final class ApplicationController extends BaseController {

    private final Jophiel jophiel;
    private final UserService userService;

    public ApplicationController(Jophiel jophiel, UserService userService) {
        this.jophiel = jophiel;
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
            return redirect(org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.login(returnUri));
        }
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public Result afterLogin(String returnUri) {
        if (session().containsKey("role")) {
            JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());

            if (JudgelsUtils.hasViewPoint()) {
                try {
                    SandalphonUtils.backupSession();
                    SandalphonUtils.setUserSession(jophiel.getUserByUserJid(JudgelsUtils.getViewPoint()), userService.findUserByUserJid(JudgelsUtils.getViewPoint()));
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
    @Transactional
    public Result postViewAs() {
        Form<ViewpointForm> form = Form.form(ViewpointForm.class).bindFromRequest();

        if ((!(form.hasErrors() || form.hasGlobalErrors())) && (SandalphonUtils.trullyHasRole("admin"))) {
            ViewpointForm viewpointForm = form.get();
            try {
                String userJid = jophiel.verifyUsername(viewpointForm.username);
                if (userJid != null) {
                    try {
                        userService.upsertUserFromJophielUserJid(userJid);
                        if (!JudgelsUtils.hasViewPoint()) {
                            SandalphonUtils.backupSession();
                        }
                        JudgelsUtils.setViewPointInSession(userJid);
                        SandalphonUtils.setUserSession(jophiel.getUserByUserJid(userJid), userService.findUserByUserJid(userJid));

                        ControllerUtils.getInstance().addActivityLog("View as user " + viewpointForm.username + ".");

                    } catch (IOException e) {
                        JudgelsUtils.removeViewPoint();
                        SandalphonUtils.restoreSession();
                    }
                }
            } catch (IOException e) {
                // do nothing
                e.printStackTrace();
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
}
