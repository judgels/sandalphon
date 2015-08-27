package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.services.LessonService;
import play.i18n.Messages;

public final class LessonStatementControllerUtils {

    private LessonStatementControllerUtils() {
        // prevent instantiation
    }

    static void appendSubtabsLayout(LazyHtml content, LessonService lessonService, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("commons.view"), routes.LessonStatementController.viewStatement(lesson.getId())));

        if (LessonControllerUtils.isAllowedToUpdateStatement(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("commons.update"), routes.LessonStatementController.updateStatement(lesson.getId())));
        }

        internalLinks.add(new InternalLink(Messages.get("lesson.statement.media"), routes.LessonStatementController.listStatementMediaFiles(lesson.getId())));

        if (LessonControllerUtils.isAllowedToManageStatementLanguages(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.statement.language"), routes.LessonStatementController.listStatementLanguages(lesson.getId())));
        }

        content.appendLayout(c -> subtabLayout.render(internalLinks.build(), c));
    }

    static void appendBreadcrumbsLayout(LazyHtml content, Lesson lesson, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                LessonControllerUtils.getLessonBreadcrumbsBuilder(lesson)
                        .add(new InternalLink(Messages.get("lesson.statement"), routes.LessonController.jumpToStatement(lesson.getId())))
                        .add(lastLink)
                        .build()
        );
    }
}
