package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.EnumUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemService;
import org.iatoki.judgels.sandalphon.bundle.BundleItemType;
import org.iatoki.judgels.sandalphon.bundle.BundleProblemService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.bundle.item.ItemCreateForm;
import org.iatoki.judgels.sandalphon.views.html.bundle.item.listCreateItemsView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class BundleItemController extends BaseController {
    private static final long PAGE_SIZE = 20;

    private final ProblemService problemService;
    private final BundleProblemService bundleProblemService;
    private final BundleItemService bundleItemService;

    public BundleItemController(ProblemService problemService, BundleProblemService bundleProblemService, BundleItemService bundleItemService) {
        this.problemService = problemService;
        this.bundleProblemService = bundleProblemService;
        this.bundleItemService = bundleItemService;
    }

    public Result viewItems(long problemId) throws ProblemNotFoundException  {
        return listCreateItems(problemId, 0, "id", "desc", "");
    }

    public Result listCreateItems(long problemId, long pageIndex, String orderBy, String orderDir, String filterString) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), pageIndex, PAGE_SIZE, orderBy, orderDir, filterString);
                Form<ItemCreateForm> form = Form.form(ItemCreateForm.class);

                return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    @AddCSRFToken
    public Result createItem(long problemId, String itemType, long page, String orderBy, String orderDir, String filterString) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                if (EnumUtils.isValidEnum(BundleItemType.class, itemType)) {
                    BundleItemConfAdapter adapter = BundleItemConfAdapters.fromItemType(BundleItemType.valueOf(itemType));
                    if (adapter != null) {
                        return showCreateItem(problem, itemType, adapter.getConfHtml(adapter.generateForm(), routes.BundleItemController.postCreateItem(problem.getId(), itemType, page, orderBy, orderDir, filterString), Messages.get("commons.create")), page, orderBy, orderDir, filterString);
                    } else {
                        Form<ItemCreateForm> form = Form.form(ItemCreateForm.class);
                        form.reject("error.problem.bundle.item.undefined");
                        Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), page, PAGE_SIZE, orderBy, orderDir, filterString);

                        return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
                    }
                } else {
                    Form<ItemCreateForm> form = Form.form(ItemCreateForm.class);
                    form.reject("error.problem.bundle.item.undefined");
                    Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), page, PAGE_SIZE, orderBy, orderDir, filterString);

                    return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postCreateItem(long problemId, String itemType, long page, String orderBy, String orderDir, String filterString) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                if (EnumUtils.isValidEnum(BundleItemType.class, itemType)) {
                    BundleItemConfAdapter adapter = BundleItemConfAdapters.fromItemType(BundleItemType.valueOf(itemType));
                    if (adapter != null) {
                        Form form = adapter.bindFormFromRequest(request());
                        if (form.hasErrors() || form.hasGlobalErrors()) {
                            return showCreateItem(problem, itemType, adapter.getConfHtml(adapter.generateForm(), routes.BundleItemController.postCreateItem(problem.getId(), itemType, page, orderBy, orderDir, filterString), Messages.get("commons.create")), page, orderBy, orderDir, filterString);
                        } else {
                            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                            if (!bundleItemService.existByMeta(problem.getJid(), IdentityUtils.getUserJid(), adapter.getMetaFromForm(form))) {
                                bundleItemService.createItem(problem.getJid(), IdentityUtils.getUserJid(), BundleItemType.valueOf(itemType), adapter.getMetaFromForm(form), adapter.processRequestForm(form), ProblemControllerUtils.getDefaultStatementLanguage(problemService, problem));

                                return redirect(routes.BundleItemController.viewItems(problem.getId()));
                            } else {
                                form.reject("error.problem.bundle.item.duplicateMeta");
                                Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), page, PAGE_SIZE, orderBy, orderDir, filterString);

                                return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
                            }
                        }
                    } else {
                        Form<ItemCreateForm> form = Form.form(ItemCreateForm.class);
                        form.reject("error.problem.bundle.item.undefined");
                        Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), page, PAGE_SIZE, orderBy, orderDir, filterString);

                        return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
                    }
                } else {
                    Form<ItemCreateForm> form = Form.form(ItemCreateForm.class);
                    form.reject("error.problem.bundle.item.undefined");
                    Page<BundleItem> items = bundleItemService.pageItems(problem.getJid(), IdentityUtils.getUserJid(), page, PAGE_SIZE, orderBy, orderDir, filterString);

                    return showListCreateItems(problem, items, orderBy, orderDir, filterString, form);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    @AddCSRFToken
    public Result updateItem(long problemId, String itemJid) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToUpdateItemInLanguage(problemService, problem)) {
            try {
                if (bundleItemService.existByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid)) {
                    BundleItem bundleItem = bundleItemService.findByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid);
                    BundleItemConfAdapter adapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
                    Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
                    if (adapter != null) {
                        Form form;
                        try {
                            form = adapter.generateForm(bundleItemService.getItemConfByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid, ProblemControllerUtils.getCurrentStatementLanguage()), bundleItem.getMeta());
                        } catch (IOException e) {
                            form = adapter.generateForm(bundleItemService.getItemConfByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid, ProblemControllerUtils.getDefaultStatementLanguage(problemService, problem)), bundleItem.getMeta());
                        }

                        return showUpdateItem(problem, bundleItem, adapter.getConfHtml(form, routes.BundleItemController.postUpdateItem(problem.getId(), itemJid), Messages.get("commons.update")), allowedLanguages);
                    } else {
                        return notFound();
                    }
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postUpdateItem(long problemId, String itemJid) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToUpdateItemInLanguage(problemService, problem)) {
            try {
                if (bundleItemService.existByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid)) {
                    BundleItem bundleItem = bundleItemService.findByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid);
                    BundleItemConfAdapter adapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
                    Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
                    if (adapter != null) {
                        Form form = adapter.bindFormFromRequest(request());
                        if (form.hasErrors() || form.hasGlobalErrors()) {
                            return showUpdateItem(problem, bundleItem, adapter.getConfHtml(adapter.generateForm(), routes.BundleItemController.postUpdateItem(problem.getId(), itemJid), Messages.get("commons.update")), allowedLanguages);
                        } else {
                            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());
                            bundleItemService.updateItem(problem.getJid(), IdentityUtils.getUserJid(), itemJid, adapter.getMetaFromForm(form), adapter.processRequestForm(form), ProblemControllerUtils.getCurrentStatementLanguage());

                            return redirect(routes.BundleItemController.viewItems(problem.getId()));
                        }
                    } else {
                        System.out.println("APA 1");
                        return notFound();
                    }
                } else {
                    System.out.println("APA 2");
                    return notFound();
                }
            } catch (IOException e) {
                System.out.println("APA 3");
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("APA 4");
            return notFound();
        }
    }

    public Result moveItemUp(long problemId, String itemJid) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                if (bundleItemService.existByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid)) {
                    bundleItemService.moveItemUp(problem.getJid(), IdentityUtils.getUserJid(), itemJid);

                    return redirect(routes.BundleItemController.viewItems(problem.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    public Result moveItemDown(long problemId, String itemJid) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                if (bundleItemService.existByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid)) {
                    bundleItemService.moveItemDown(problem.getJid(), IdentityUtils.getUserJid(), itemJid);

                    return redirect(routes.BundleItemController.viewItems(problem.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    public Result removeItem(long problemId, String itemJid) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (BundleProblemControllerUtils.isAllowedToManageItems(problemService, problem)) {
            try {
                if (bundleItemService.existByItemJid(problem.getJid(), IdentityUtils.getUserJid(), itemJid)) {
                    bundleItemService.removeItem(problem.getJid(), IdentityUtils.getUserJid(), itemJid);

                    return redirect(routes.BundleItemController.viewItems(problem.getId()));
                } else {
                    return notFound();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return notFound();
        }
    }

    private Result showListCreateItems(Problem problem, Page<BundleItem> currentPage, String orderBy, String orderDir, String filterString, Form<ItemCreateForm> form) {
        LazyHtml content = new LazyHtml(listCreateItemsView.render(currentPage, problem.getId(), currentPage.getPageIndex(), orderBy, orderDir, filterString, form));

        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, ImmutableList.of(
              new InternalLink(Messages.get("problem.bundle.item.list"), routes.BundleItemController.viewItems(problem.getId()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Bundle - Items");

        ControllerUtils.getInstance().addActivityLog("List items of programming problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showCreateItem(Problem problem, String itemType, Html html, long page, String orderBy, String orderDir, String filterString) {
        LazyHtml content = new LazyHtml(html);
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, ImmutableList.of(
              new InternalLink(Messages.get("problem.bundle.item.list"), routes.BundleItemController.viewItems(problem.getId())),
              new InternalLink(Messages.get("problem.bundle.item.create"), routes.BundleItemController.createItem(problem.getId(), itemType, page, orderBy, orderDir, filterString))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Bundle - Items - Create");

        ControllerUtils.getInstance().addActivityLog("Try to create bundle item on " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdateItem(Problem problem, BundleItem bundleItem, Html html, Set<String> allowedLanguages) {
        LazyHtml content = new LazyHtml(html);
        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemController.switchLanguage(problem.getId()));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, ImmutableList.of(
              new InternalLink(Messages.get("problem.bundle.item.list"), routes.BundleItemController.viewItems(problem.getId())),
              new InternalLink(Messages.get("problem.bundle.item.update"), routes.BundleItemController.updateItem(problem.getId(), bundleItem.getJid()))
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Bundle - Item - Update");

        ControllerUtils.getInstance().addActivityLog("Try to update bundle item on " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, List<InternalLink> lastLinks) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                    .add(new InternalLink(Messages.get("problem.bundle.item"), routes.BundleProblemController.jumpToItems(problem.getId())))
                    .addAll(lastLinks)
                    .build()
        );
    }
}
