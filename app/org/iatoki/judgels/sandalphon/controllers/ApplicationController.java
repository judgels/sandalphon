package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.forms.ViewpointForm;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.UserService;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Named
public final class ApplicationController extends AbstractJudgelsController {

    private final Jophiel jophiel;
    private final UserService userService;

    @Inject
    public ApplicationController(Jophiel jophiel, UserService userService) {
        this.jophiel = jophiel;
        this.userService = userService;
    }

    public Result index() {
        if (session().containsKey("username") && session().containsKey("role")) {
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
        if (session().containsKey("username") && session().containsKey("role")) {
            return redirect(returnUri);
        } else if (session().containsKey("username")) {
            return redirect(routes.ApplicationController.authRole(returnUri));
        } else {
            String newReturnUri = org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterLogin(returnUri).absoluteURL(request(), request().secure());
            return redirect(org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.login(newReturnUri));
        }
    }

    @Transactional
    public Result authRole(String returnUri) {
        if (session().containsKey("username") && session().containsKey("role")) {
            return redirect(returnUri);
        }

        String userRoleJid = IdentityUtils.getUserJid();
        if (!userService.existsByUserJid(userRoleJid)) {
            userService.createUser(userRoleJid, SandalphonUtils.getDefaultRoles());
            SandalphonUtils.saveRolesInSession(SandalphonUtils.getDefaultRoles());
            return redirect(returnUri);
        }

        User userRole = userService.findUserByJid(userRoleJid);
        SandalphonUtils.saveRolesInSession(userRole.getRoles());
        return redirect(returnUri);
    }

    @Transactional
    public Result afterLogin(String returnUri) {
        if (!session().containsKey("role")) {
            String newReturnUri = org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterLogin(returnUri).absoluteURL(request(), request().secure());
            return redirect(routes.ApplicationController.authRole(newReturnUri));
        }

        JudgelsPlayUtils.updateUserJidCache(JidCacheServiceImpl.getInstance());

        if (JudgelsPlayUtils.hasViewPoint()) {
            try {
                SandalphonUtils.backupSession();
                SandalphonUtils.setUserSession(jophiel.getUserByUserJid(JudgelsPlayUtils.getViewPoint()), userService.findUserByJid(JudgelsPlayUtils.getViewPoint()));
            } catch (IOException e) {
                JudgelsPlayUtils.removeViewPoint();
                SandalphonUtils.restoreSession();
            }
        }

        return redirect(returnUri);
    }

    @Authenticated(value = {LoggedIn.class, HasRole.class})
    @Transactional
    public Result postViewAs() {
        Form<ViewpointForm> viewPointForm = Form.form(ViewpointForm.class).bindFromRequest();

        if (!formHasErrors(viewPointForm) && SandalphonUtils.trullyHasRole("admin")) {
            ViewpointForm viewpointData = viewPointForm.get();
            try {
                String userJid = jophiel.verifyUsername(viewpointData.username);
                if (userJid != null) {
                    try {
                        userService.upsertUserFromJophielUserJid(userJid);
                        if (!JudgelsPlayUtils.hasViewPoint()) {
                            SandalphonUtils.backupSession();
                        }
                        JudgelsPlayUtils.setViewPointInSession(userJid);
                        SandalphonUtils.setUserSession(jophiel.getUserByUserJid(userJid), userService.findUserByJid(userJid));

                        SandalphonControllerUtils.getInstance().addActivityLog("View as user " + viewpointData.username + ".");

                    } catch (IOException e) {
                        JudgelsPlayUtils.removeViewPoint();
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
        JudgelsPlayUtils.removeViewPoint();
        SandalphonUtils.restoreSession();

        return redirect(request().getHeader("Referer"));
    }
}
