package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.baseLayout;
import org.iatoki.judgels.commons.views.html.layouts.breadcrumbsLayout;
import org.iatoki.judgels.commons.views.html.layouts.headerFooterLayout;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.commons.views.html.layouts.leftSidebarLayout;
import org.iatoki.judgels.commons.views.html.auth.authView;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

public final class Application extends Controller {

    public Result index() {
        String returnUri = routes.ProblemController.index().absoluteURL(request());
        return redirect(routes.Application.auth(returnUri));
    }

    public Result auth(String returnUri) {
        LazyHtml content = new LazyHtml(authView.render(org.iatoki.judgels.commons.controllers.routes.JophielClientController.login(returnUri)));
        content.appendLayout(c -> headingLayout.render(Messages.get("commons.login.heading.login"), c));
        content.appendLayout(c -> breadcrumbsLayout.render(ImmutableList.of(), c));
        appendTemplateLayout(content);
        return getResult(content, Http.Status.OK);
    }

    private void appendTemplateLayout(LazyHtml content) {
        content.appendLayout(c -> leftSidebarLayout.render(
                IdentityUtils.getUsername(),
                IdentityUtils.getUserRealName(),
                "#",
                org.iatoki.judgels.commons.controllers.routes.JophielClientController.logout(routes.Application.index().absoluteURL(request())).absoluteURL(request()),
                ImmutableList.of(), c)
        );

        content.appendLayout(c -> headerFooterLayout.render(c));
        content.appendLayout(c -> baseLayout.render("TODO", c));
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
