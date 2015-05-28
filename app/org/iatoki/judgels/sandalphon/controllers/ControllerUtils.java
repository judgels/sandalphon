package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.ViewpointForm;
import org.iatoki.judgels.commons.controllers.AbstractControllerUtils;
import org.iatoki.judgels.commons.views.html.layouts.menusLayout;
import org.iatoki.judgels.commons.views.html.layouts.profileView;
import org.iatoki.judgels.commons.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.commons.views.html.layouts.viewAsLayout;
import org.iatoki.judgels.jophiel.commons.JophielUtils;
import org.iatoki.judgels.jophiel.commons.UserActivity;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.UserActivityServiceImpl;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;

public final class ControllerUtils extends AbstractControllerUtils {

    private static ControllerUtils INSTANCE = new ControllerUtils();

    @Override
    public void appendSidebarLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();

        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()));
        internalLinkBuilder.add(new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index()));
        if (isAdmin()) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("user.users"), routes.UserController.index()));
        }

        LazyHtml sidebarContent = new LazyHtml(profileView.render(
              IdentityUtils.getUsername(),
              IdentityUtils.getUserRealName(),
              org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterProfile(routes.ProblemController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()),
              org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())
        ));
        if (SandalphonUtils.trullyHasRole("admin")) {
            Form<ViewpointForm> form = Form.form(ViewpointForm.class);
            if (JudgelsUtils.hasViewPoint()) {
                ViewpointForm viewpointForm = new ViewpointForm();
                viewpointForm.username = IdentityUtils.getUsername();
                form.fill(viewpointForm);
            }
            sidebarContent.appendLayout(c -> viewAsLayout.render(form, JophielUtils.getAutoCompleteEndPoint(), "javascripts/userAutoComplete.js", org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.postViewAs(), org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.resetViewAs(), c));
        }
        sidebarContent.appendLayout(c -> menusLayout.render(internalLinkBuilder.build(), c));
        content.appendLayout(c -> sidebarLayout.render(sidebarContent.render(), c));
    }

    boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    public void addActivityLog(String log) {
        try {
            if (JudgelsUtils.hasViewPoint()) {
                log += " view as " +  IdentityUtils.getUserJid();
            }
            UserActivityServiceImpl.getInstance().addUserActivity(new UserActivity(System.currentTimeMillis(), SandalphonUtils.getRealUserJid(), log, IdentityUtils.getIpAddress()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static ControllerUtils getInstance() {
        return INSTANCE;
    }
}
