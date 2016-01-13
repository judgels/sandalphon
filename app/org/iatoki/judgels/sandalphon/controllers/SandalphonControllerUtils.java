package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.jophiel.ActivityKey;
import org.iatoki.judgels.jophiel.UserActivityMessage;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielPublicAPI;
import org.iatoki.judgels.jophiel.controllers.JophielClientControllerUtils;
import org.iatoki.judgels.jophiel.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.jophiel.forms.SearchProfileForm;
import org.iatoki.judgels.jophiel.forms.ViewpointForm;
import org.iatoki.judgels.jophiel.views.html.client.linkedClientsLayout;
import org.iatoki.judgels.jophiel.views.html.isLoggedInLayout;
import org.iatoki.judgels.jophiel.views.html.isLoggedOutLayout;
import org.iatoki.judgels.jophiel.views.html.profile.searchProfileLayout;
import org.iatoki.judgels.jophiel.views.html.viewas.viewAsLayout;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsControllerUtils;
import org.iatoki.judgels.play.controllers.ControllerUtils;
import org.iatoki.judgels.play.views.html.layouts.contentLayout;
import org.iatoki.judgels.play.views.html.layouts.menusLayout;
import org.iatoki.judgels.play.views.html.layouts.profileView;
import org.iatoki.judgels.play.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.services.impls.ActivityLogServiceImpl;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;

public final class SandalphonControllerUtils extends AbstractJudgelsControllerUtils {

    private static SandalphonControllerUtils INSTANCE;

    private final JophielClientAPI jophielClientAPI;
    private final JophielPublicAPI jophielPublicAPI;

    public SandalphonControllerUtils(JophielClientAPI jophielClientAPI, JophielPublicAPI jophielPublicAPI) {
        this.jophielClientAPI = jophielClientAPI;
        this.jophielPublicAPI = jophielPublicAPI;
    }

    @Override
    public void appendSidebarLayout(LazyHtml content) {
        content.appendLayout(c -> contentLayout.render(c));

        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();

        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), org.iatoki.judgels.sandalphon.problem.base.routes.ProblemController.index()));
        internalLinkBuilder.add(new InternalLink(Messages.get("lesson.lessons"), org.iatoki.judgels.sandalphon.lesson.routes.LessonController.index()));
        if (isAdmin()) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), org.iatoki.judgels.sandalphon.client.routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), org.iatoki.judgels.sandalphon.grader.routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("user.users"), org.iatoki.judgels.sandalphon.user.routes.UserController.index()));
        }

        LazyHtml sidebarContent = new LazyHtml(profileView.render(
                IdentityUtils.getUsername(),
                IdentityUtils.getUserRealName(),
                org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.profile().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()),
                org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())
        ));
        if (SandalphonUtils.trullyHasRole("admin")) {
            Form<ViewpointForm> form = Form.form(ViewpointForm.class);
            if (JudgelsPlayUtils.hasViewPoint()) {
                ViewpointForm viewpointForm = new ViewpointForm();
                viewpointForm.username = IdentityUtils.getUsername();
                form.fill(viewpointForm);
            }
            sidebarContent.appendLayout(c -> viewAsLayout.render(form, jophielPublicAPI.getUserAutocompleteAPIEndpoint(), "lib/jophielcommons/javascripts/userAutoComplete.js", org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.postViewAs(), org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.resetViewAs(), c));
        }
        sidebarContent.appendLayout(c -> menusLayout.render(internalLinkBuilder.build(), c));
        sidebarContent.appendLayout(c -> linkedClientsLayout.render(jophielClientAPI.getLinkedClientsAPIEndpoint(), "lib/jophielcommons/javascripts/linkedClients.js", c));
        Form<SearchProfileForm> searchProfileForm = Form.form(SearchProfileForm.class);
        sidebarContent.appendLayout(c -> searchProfileLayout.render(searchProfileForm, jophielPublicAPI.getUserAutocompleteAPIEndpoint(), "lib/jophielcommons/javascripts/userAutoComplete.js", JophielClientControllerUtils.getInstance().getUserSearchProfileUrl(), c));

        content.appendLayout(c -> sidebarLayout.render(sidebarContent.render(), c));
        if (IdentityUtils.getUserJid() == null) {
            content.appendLayout(c -> isLoggedInLayout.render(jophielClientAPI.getUserIsLoggedInAPIEndpoint(), routes.ApplicationController.auth(ControllerUtils.getCurrentUrl(Http.Context.current().request())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()), "lib/jophielcommons/javascripts/isLoggedIn.js", c));
        } else {
            content.appendLayout(c -> isLoggedOutLayout.render(jophielClientAPI.getUserIsLoggedInAPIEndpoint(), routes.ApplicationController.logout(ControllerUtils.getCurrentUrl(Http.Context.current().request())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()), "lib/jophielcommons/javascripts/isLoggedOut.js", SandalphonUtils.getRealUserJid(), c));
        }
    }

    public boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    public void addActivityLog(ActivityKey activityKey) {
        long time = System.currentTimeMillis();
        ActivityLogServiceImpl.getInstance().addActivityLog(activityKey, SandalphonUtils.getRealUsername(), time, SandalphonUtils.getRealUserJid(), IdentityUtils.getIpAddress());
        String log = SandalphonUtils.getRealUsername() + " " + activityKey.toString();
        try {
            if (JudgelsPlayUtils.hasViewPoint()) {
                log += " view as " +  IdentityUtils.getUsername();
            }
            UserActivityMessageServiceImpl.getInstance().addUserActivityMessage(new UserActivityMessage(System.currentTimeMillis(), SandalphonUtils.getRealUserJid(), log, IdentityUtils.getIpAddress()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void buildInstance(JophielClientAPI jophielClientAPI, JophielPublicAPI jophielPublicAPI) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("SandalphonControllerUtils instance has already been built");
        }
        INSTANCE = new SandalphonControllerUtils(jophielClientAPI, jophielPublicAPI);
    }

    public static SandalphonControllerUtils getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("SandalphonControllerUtils instance has not been built");
        }
        return INSTANCE;
    }
}
