package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.heading3Layout;
import org.iatoki.judgels.play.views.html.layouts.heading3WithActionLayout;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.PublicUser;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonPartner;
import org.iatoki.judgels.sandalphon.LessonPartnerConfig;
import org.iatoki.judgels.sandalphon.LessonPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.LessonPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.LessonPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.LessonPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.listPartnersView;
import org.iatoki.judgels.sandalphon.views.html.lesson.partner.updatePartnerView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Set;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public class LessonPartnerController extends AbstractJudgelsController {

    private static final long PAGE_SIZE = 20;

    private final Jophiel jophiel;
    private final LessonService lessonService;

    @Inject
    public LessonPartnerController(Jophiel jophiel, LessonService lessonService) {
        this.jophiel = jophiel;
        this.lessonService = lessonService;
    }

    @Transactional(readOnly = true)
    public Result viewPartners(long lessonId) throws LessonNotFoundException {
        return listPartners(lessonId, 0, "id", "desc");
    }

    @Transactional(readOnly = true)
    public Result listPartners(long lessonId, long pageIndex, String orderBy, String orderDir) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAuthorOrAbove(lesson)) {
            return notFound();
        }

        Page<LessonPartner> pageOfLessonPartners = lessonService.getPageOfLessonPartners(lesson.getJid(), pageIndex, PAGE_SIZE, orderBy, orderDir);

        LazyHtml content = new LazyHtml(listPartnersView.render(lesson.getId(), pageOfLessonPartners, orderBy, orderDir));
        content.appendLayout(c -> heading3WithActionLayout.render(Messages.get("lesson.partner.list"), new InternalLink(Messages.get("lesson.partner.add"), routes.LessonPartnerController.addPartner(lesson.getId())), c));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.list"), routes.LessonPartnerController.viewPartners(lesson.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Partners");

        SandalphonControllerUtils.getInstance().addActivityLog("Open all partners <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result addPartner(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAuthorOrAbove(lesson)) {
            return notFound();
        }

        Form<LessonPartnerUsernameForm> usernameForm = Form.form(LessonPartnerUsernameForm.class);
        Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to add partner of lesson " + lesson.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showAddPartner(usernameForm, lessonForm, lesson);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postAddPartner(long lessonId) throws LessonNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAuthorOrAbove(lesson)) {
            return notFound();
        }

        Form<LessonPartnerUsernameForm> usernameForm = Form.form(LessonPartnerUsernameForm.class).bindFromRequest();
        Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(usernameForm) || formHasErrors(lessonForm)) {
            return showAddPartner(usernameForm, lessonForm, lesson);
        }

        String username = usernameForm.get().username;
        LessonPartnerUpsertForm lessonData = lessonForm.get();

        String userJid;
        try {
            userJid = jophiel.verifyUsername(username);
        } catch (IOException e) {
            return notFound();
        }

        if (userJid == null) {
            usernameForm.reject("username", Messages.get("lesson.partner.usernameNotFound"));
            return showAddPartner(usernameForm, lessonForm, lesson);
        }

        PublicUser publicUser;
        try {
            publicUser = jophiel.getPublicUserByJid(userJid);
        } catch (IOException e) {
            return notFound();
        }

        JidCacheServiceImpl.getInstance().putDisplayName(publicUser.getJid(), JudgelsPlayUtils.getUserDisplayName(publicUser.getUsername()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        if (lessonService.isUserPartnerForLesson(lesson.getJid(), userJid)) {
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

        SandalphonControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of lesson " + lesson.getSlug() + ".");

        return redirect(routes.LessonPartnerController.viewPartners(lesson.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updatePartner(long lessonId, long partnerId) throws LessonNotFoundException, LessonPartnerNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAuthorOrAbove(lesson)) {
            return notFound();
        }

        LessonPartner lessonPartner = lessonService.findLessonPartnerById(partnerId);

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

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update partner " + lessonPartner.getPartnerJid() + " of lesson " + lesson.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdatePartner(lessonForm, lesson, lessonPartner);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdatePartner(long lessonId, long partnerId) throws LessonNotFoundException, LessonPartnerNotFoundException {
        Lesson lesson = lessonService.findLessonById(lessonId);

        if (!LessonControllerUtils.isAuthorOrAbove(lesson)) {
            return notFound();
        }

        LessonPartner lessonPartner = lessonService.findLessonPartnerById(partnerId);

        Form<LessonPartnerUpsertForm> lessonForm = Form.form(LessonPartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(lessonForm)) {
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

        SandalphonControllerUtils.getInstance().addActivityLog("Update partner " + lessonPartner.getPartnerJid() + " of lesson " + lesson.getSlug() + ".");

        return redirect(routes.LessonPartnerController.updatePartner(lesson.getId(), lessonPartner.getId()));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
              LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                    .add(new InternalLink(Messages.get("lesson.partner"), routes.LessonController.jumpToPartners(lesson.getId())))
                    .add(lastLink)
                    .build()
        );
    }

    private Result showAddPartner(Form<LessonPartnerUsernameForm> usernameForm, Form<LessonPartnerUpsertForm> lessonForm, Lesson lesson) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, lessonForm, lesson, jophiel.getAutoCompleteEndPoint()));

        content.appendLayout(c -> heading3Layout.render(Messages.get("lesson.partner.add"), c));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.add"), routes.LessonPartnerController.addPartner(lesson.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Add Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<LessonPartnerUpsertForm> lessonForm, Lesson lesson, LessonPartner lessonPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(lessonForm, lesson, lessonPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("lesson.partner.update") + ": " + JidCacheServiceImpl.getInstance().getDisplayName(lessonPartner.getPartnerJid()), c));
        LessonControllerUtils.appendTabsLayout(content, lessonService, lesson);
        LessonControllerUtils.appendVersionLocalChangesWarningLayout(content, lessonService, lesson);
        LessonControllerUtils.appendTitleLayout(content, lessonService, lesson);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, lesson, new InternalLink(Messages.get("lesson.partner.update"), routes.LessonPartnerController.updatePartner(lesson.getId(), lessonPartner.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Lesson - Update Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
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
