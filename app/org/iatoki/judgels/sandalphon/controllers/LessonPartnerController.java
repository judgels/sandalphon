package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.heading3Layout;
import org.iatoki.judgels.commons.views.html.layouts.heading3WithActionLayout;
import org.iatoki.judgels.jophiel.User;
import org.iatoki.judgels.jophiel.commons.JophielUtils;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonPartner;
import org.iatoki.judgels.sandalphon.LessonPartnerConfig;
import org.iatoki.judgels.sandalphon.LessonPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.LessonPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.LessonService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.LessonPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.LessonPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.listPartnersView;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.updatePartnerView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public class LessonPartnerController extends BaseController {
    private static final long PAGE_SIZE = 20;

    private final LessonService lessonService;

    public LessonPartnerController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    public Result viewPartners(long lessonId) throws LessonNotFoundException {
        return listPartners(lessonId, 0, "id", "desc");
    }

    public Result listPartners(long lessonId, long pageIndex, String orderBy, String orderDir) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            Page<LessonPartner> partners = lessonService.pageLessonPartners(lesson.getJid(), pageIndex, PAGE_SIZE, orderBy, orderDir);

            LazyHtml content = new LazyHtml(listPartnersView.render(lesson.getId(), partners, orderBy, orderDir));
            content.appendLayout(c -> heading3WithActionLayout.render(Messages.get("lesson.partner.list"), new InternalLink(Messages.get("lesson.partner.add"), routes.LessonPartnerController.addPartner(lesson.getId())), c));
            LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
            LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
            LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.list"), routes.LessonPartnerController.viewPartners(lesson.getId())));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Partners");

            ControllerUtils.getInstance().addActivityLog("Open all partners <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    @AddCSRFToken
    public Result addPartner(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            Form<LessonPartnerUsernameForm> usernameForm = Form.form(LessonPartnerUsernameForm.class);
            Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class);

            ControllerUtils.getInstance().addActivityLog("Try to add partner of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showAddPartner(usernameForm, lessonForm, lesson);
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postAddPartner(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            Form<LessonPartnerUsernameForm> usernameForm = Form.form(LessonPartnerUsernameForm.class).bindFromRequest();
            Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class).bindFromRequest();

            if (usernameForm.hasErrors() || usernameForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, lessonForm, lesson);
            }

            if (lessonForm.hasErrors() || lessonForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, lessonForm, lesson);
            }

            String username = usernameForm.get().username;
            LessonPartnerUpsertForm lessonData = lessonForm.get();

            String userJid = JophielUtils.verifyUsername(username);
            if (userJid == null) {
                usernameForm.reject("username", Messages.get("lesson.partner.usernameNotFound"));
                return showAddPartner(usernameForm, lessonForm, lesson);
            }

            try {
                User user = JophielUtils.getUserByUserJid(userJid);
                JidCacheService.getInstance().putDisplayName(user.getJid(), JudgelsUtils.getUserDisplayName(user.getUsername(), user.getName()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

                if (lessonService.isLessonPartnerByUserJid(lesson.getJid(), userJid)) {
                    usernameForm.reject("username", Messages.get("lesson.partner.already"));
                    return showAddPartner(usernameForm, lessonForm, lesson);
                }

                LessonPartnerConfig partnerConfig = new LessonPartnerConfigBuilder()
                      .setIsAllowedToUpdateLesson(lessonData.isAllowedToUpdateLesson)
                      .setIsAllowedToUpdateStatement(lessonData.isAllowedToUpdateStatement)
                      .setIsAllowedToUploadStatementResources(lessonData.isAllowedToUploadStatementResources)
                      .setAllowedStatementLanguagesToView(splitByComma(lessonData.allowedStatementLanguagesToView))
                      .setAllowedStatementLanguagesToUpdate(splitByComma(lessonData.allowedStatementLanguagesToUpdate))
                      .setIsAllowedToManageStatementLanguages(lessonData.isAllowedToManageStatementLanguages)
                      .setIsAllowedToViewVersionHistory(lessonData.isAllowedToViewVersionHistory)
                      .setIsAllowedToRestoreVersionHistory(lessonData.isAllowedToRestoreVersionHistory)
                      .setIsAllowedToManageLessonClients(lessonData.isAllowedToManageLessonClients)
                      .build();

                lessonService.createLessonPartner(lesson.getId(), userJid, partnerConfig);

                ControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of lesson " + lesson.getName() + ".");

                return redirect(routes.LessonPartnerController.viewPartners(lesson.getId()));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    @AddCSRFToken
    public Result updatePartner(long lessonId, long partnerId) throws LessonNotFoundException, LessonPartnerNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            LessonPartner lessonPartner = lessonService.findLessonPartnerByLessonPartnerId(partnerId);

            LessonPartnerConfig lessonConfig = lessonPartner.getConfig();
            LessonPartnerUpsertForm lessonData = new LessonPartnerUpsertForm();

            lessonData.isAllowedToUpdateLesson = lessonConfig.isAllowedToUpdateLesson();
            lessonData.isAllowedToUpdateStatement = lessonConfig.isAllowedToUpdateStatement();
            lessonData.isAllowedToUploadStatementResources = lessonConfig.isAllowedToUploadStatementResources();
            lessonData.allowedStatementLanguagesToView = combineByComma(lessonConfig.getAllowedStatementLanguagesToView());
            lessonData.allowedStatementLanguagesToUpdate = combineByComma(lessonConfig.getAllowedStatementLanguagesToUpdate());
            lessonData.isAllowedToManageStatementLanguages = lessonConfig.isAllowedToManageStatementLanguages();
            lessonData.isAllowedToViewVersionHistory = lessonConfig.isAllowedToViewVersionHistory();
            lessonData.isAllowedToRestoreVersionHistory = lessonConfig.isAllowedToRestoreVersionHistory();
            lessonData.isAllowedToManageLessonClients = lessonConfig.isAllowedToManageLessonClients();

            Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class).fill(lessonData);

            ControllerUtils.getInstance().addActivityLog("Try to update partner " + lessonPartner.getPartnerJid() + " of lesson " + lesson.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdatePartner(lessonForm, lesson, lessonPartner);
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postUpdatePartner(long lessonId, long partnerId) throws LessonNotFoundException, LessonPartnerNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            LessonPartner lessonPartner = lessonService.findLessonPartnerByLessonPartnerId(partnerId);

            Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class).bindFromRequest();

            if (lessonForm.hasErrors() || lessonForm.hasGlobalErrors()) {
                return showUpdatePartner(lessonForm, lesson, lessonPartner);
            }

            LessonPartnerUpsertForm lessonData = lessonForm.get();

            LessonPartnerConfig lessonConfig = new LessonPartnerConfigBuilder()
                  .setIsAllowedToUpdateLesson(lessonData.isAllowedToUpdateLesson)
                  .setIsAllowedToUpdateStatement(lessonData.isAllowedToUpdateStatement)
                  .setIsAllowedToUploadStatementResources(lessonData.isAllowedToUploadStatementResources)
                  .setAllowedStatementLanguagesToView(splitByComma(lessonData.allowedStatementLanguagesToView))
                  .setAllowedStatementLanguagesToUpdate(splitByComma(lessonData.allowedStatementLanguagesToUpdate))
                  .setIsAllowedToManageStatementLanguages(lessonData.isAllowedToManageStatementLanguages)
                  .setIsAllowedToViewVersionHistory(lessonData.isAllowedToViewVersionHistory)
                  .setIsAllowedToRestoreVersionHistory(lessonData.isAllowedToRestoreVersionHistory)
                  .setIsAllowedToManageLessonClients(lessonData.isAllowedToManageLessonClients)
                  .build();

            lessonService.updateLessonPartner(partnerId, lessonConfig);

            ControllerUtils.getInstance().addActivityLog("Update partner " + lessonPartner.getPartnerJid() + " of lesson " + lesson.getName() + ".");

            return redirect(routes.LessonPartnerController.updatePartner(lesson.getId(), lessonPartner.getId()));
        } else {
            return notFound();
        }
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                    .add(new InternalLink(Messages.get("lesson.partner"), routes.LessonController.jumpToPartners(lesson.getId())))
                    .add(lastLink)
                    .build()
        );
    }

    private Result showAddPartner(Form<LessonPartnerUsernameForm> usernameForm, Form<LessonPartnerUpsertForm> lessonForm, Lesson lesson) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, lessonForm, lesson));

        content.appendLayout(c -> heading3Layout.render(Messages.get("lesson.partner.add"), c));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.add"), routes.LessonPartnerController.addPartner(lesson.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Add Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<LessonPartnerUpsertForm> lessonForm, Lesson lesson, LessonPartner lessonPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(lessonForm, lesson, lessonPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("lesson.partner.update") + ": " + JidCacheService.getInstance().getDisplayName(lessonPartner.getPartnerJid()), c));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.update"), routes.LessonPartnerController.updatePartner(lesson.getId(), lessonPartner.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Set<String> splitByComma(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Sets.newHashSet(s.split(","));
    }

    private String combineByComma(Set<String> list) {
        if (list == null) {
            return null;
        }
        return Joiner.on(",").join(list);
    }
}
