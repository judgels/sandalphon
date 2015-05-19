package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemService;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemService;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemStatementUtils;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.commons.views.html.bundleStatementView;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class BundleProblemStatementController extends BaseController {
    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;
    private final BundleItemService bundleItemService;

    public BundleProblemStatementController(ProblemService problemService, BundleProblemService bundleProblemService, BundleItemService bundleItemService) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
        this.bundleItemService = bundleItemService;
    }

    public Result viewStatement(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        if (ProblemControllerUtils.isAllowedToViewStatement(problemService, problem)) {
            String statement;
            try {
                statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());
            } catch (IOException e) {
                statement = BundleProblemStatementUtils.getDefaultStatement(ProblemControllerUtils.getCurrentStatementLanguage());
            }

            try {
                List<BundleItem> bundleItemList = bundleItemService.findAllItems(problem.getJid(), IdentityUtils.getUserJid());
                ImmutableList.Builder<Html> htmlBuilder = ImmutableList.builder();
                for (BundleItem bundleItem : bundleItemList) {
                    BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
                    try {
                        htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfByItemJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage())));
                    } catch (IOException e) {
                        ProblemControllerUtils.setCurrentStatementLanguage(ProblemControllerUtils.getDefaultStatementLanguage(problemService, problem));
                        htmlBuilder.add(adapter.renderViewHtml(bundleItem, bundleItemService.getItemConfByItemJid(problem.getJid(), IdentityUtils.getUserJid(), bundleItem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage())));
                    }
                }

                LazyHtml content = new LazyHtml(bundleStatementView.render(routes.BundleProblemSubmissionController.postSubmit(problemId).absoluteURL(request(), request().secure()), problem.getName(), statement, htmlBuilder.build()));

                Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToView(problemService, problem);

                ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemStatementController.viewStatementSwitchLanguage(problem.getId()));

                ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
                BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
                ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
                ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
                ControllerUtils.getInstance().appendSidebarLayout(content);
                ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.view"), routes.ProblemStatementController.viewStatement(problemId)));
                ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

                ControllerUtils.getInstance().addActivityLog("View statement of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

                return ControllerUtils.getInstance().lazyOk(content);
            } catch (IOException e) {
                e.printStackTrace();
                return notFound();
            }
        } else {
            return notFound();
        }
    }
}
