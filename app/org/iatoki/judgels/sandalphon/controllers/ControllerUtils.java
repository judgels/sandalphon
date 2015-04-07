package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.AbstractControllerUtils;
import org.iatoki.judgels.commons.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.jophiel.commons.UserActivity;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.UserActivityServiceImpl;
import play.i18n.Messages;
import play.mvc.Http;

public final class ControllerUtils extends AbstractControllerUtils {

    private static ControllerUtils INSTANCE = new ControllerUtils();

    @Override
    public void appendSidebarLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()));
        if (isAdmin()) {
            internalLinks.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinks.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinks.add(new InternalLink(Messages.get("user.users"), routes.UserController.index()));
        }
        content.appendLayout(c -> sidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterProfile(routes.ProblemController.index().absoluteURL(Http.Context.current().request())).absoluteURL(Http.Context.current().request())).absoluteURL(Http.Context.current().request()),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(Http.Context.current().request())).absoluteURL(Http.Context.current().request()),
                        internalLinks.build(), c)
        );
    }

    boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    public void addActivityLog(String log) {
        try {
            UserActivityServiceImpl.getInstance().addUserActivity(new UserActivity(System.currentTimeMillis(), IdentityUtils.getUserJid(), log, IdentityUtils.getIpAddress()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static ControllerUtils getInstance() {
        return INSTANCE;
    }
}
