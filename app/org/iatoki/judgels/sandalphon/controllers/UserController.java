package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.UserService;
import org.iatoki.judgels.sandalphon.UserUpdateForm;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.Authorized;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.views.html.user.listUsersView;
import org.iatoki.judgels.sandalphon.views.html.user.updateUserView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Arrays;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
@Authorized(value = {"admin"})
public final class UserController extends BaseController {

    private static final long PAGE_SIZE = 20;
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;

        JudgelsUtils.updateUserJidCache(JidCacheService.getInstance());
    }

    public Result index() {
        return listUsers(0, "id", "asc", "");
    }

    public Result listUsers(long page, String sortBy, String orderBy, String filterString) {
        Page<User> currentPage = userService.pageUsers(page, PAGE_SIZE, sortBy, orderBy, filterString);

        LazyHtml content = new LazyHtml(listUsersView.render(currentPage, sortBy, orderBy, filterString));
        content.appendLayout(c -> headingLayout.render(Messages.get("user.list"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("user.users"), routes.UserController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "User - List");

        ControllerUtils.getInstance().addActivityLog("List all users <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    @AddCSRFToken
    public Result updateUser(long userId) {
        User user = userService.findUserById(userId);
        UserUpdateForm userRoleUpdateForm = new UserUpdateForm();
        userRoleUpdateForm.roles = StringUtils.join(user.getRoles(), ",");
        Form<UserUpdateForm> form = Form.form(UserUpdateForm.class).fill(userRoleUpdateForm);

        ControllerUtils.getInstance().addActivityLog("Try to update user " + user.getUserJid() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdateUser(form, userId);
    }

    @RequireCSRFCheck
    public Result postUpdateUser(long userId) {
        Form<UserUpdateForm> form = Form.form(UserUpdateForm.class).bindFromRequest();
        User user = userService.findUserById(userId);

        if (form.hasErrors()) {
            return showUpdateUser(form, userId);
        } else {
            UserUpdateForm userUpdateForm = form.get();
            userService.updateUser(userId, Arrays.asList(userUpdateForm.roles.split(",")));

            ControllerUtils.getInstance().addActivityLog("Update user " + user.getUserJid() + ".");

            return redirect(routes.UserController.index());
        }
    }

    public Result deleteUser(long userId) {
        User user = userService.findUserById(userId);
        userService.deleteUser(userId);

        ControllerUtils.getInstance().addActivityLog("Delete user " + user.getUserJid() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.UserController.index());
    }

    private Result showUpdateUser(Form<UserUpdateForm> form, long userId) {
        User userRole = userService.findUserById(userId);
        LazyHtml content = new LazyHtml(updateUserView.render(form, userId));
        content.appendLayout(c -> headingLayout.render(Messages.get("user.update"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
              new InternalLink(Messages.get("user.users"), routes.UserController.index()),
              new InternalLink(Messages.get("user.update"), routes.UserController.updateUser(userId))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "User - Update");

        return ControllerUtils.getInstance().lazyOk(content);
    }

}
