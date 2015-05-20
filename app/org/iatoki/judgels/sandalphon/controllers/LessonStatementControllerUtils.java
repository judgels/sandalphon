package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.accessTypesLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonService;
import play.i18n.Messages;

public final class LessonStatementControllerUtils {
    private LessonStatementControllerUtils() {
        // prevent instantiation
    }

    public static void appendSubtabsLayout(LazyHtml content, LessonService lessonService, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.LessonStatementController.viewStatement(lesson.getId())));

        if (LessonControllerUtils.isAllowedToUpdateStatement(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.LessonStatementController.updateStatement(lesson.getId())));
        }

        internalLinks.add(new InternalLink(Messages.get("lesson.statement.media"), routes.LessonStatementController.listStatementMediaFiles(lesson.getId())));

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.statement.language"), routes.LessonStatementController.listStatementLanguages(lesson.getId())));
        }

        content.appendLayout(c -> accessTypesLayout.render(internalLinks.build(), c));
    }

    public static void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                        .add(new InternalLink(Messages.get("lesson.statement"), routes.LessonController.jumpToStatement(lesson.getId())))
                        .add(lastLink)
                        .build()
        );
    }
}
