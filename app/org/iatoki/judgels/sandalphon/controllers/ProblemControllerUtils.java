package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;

import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.commons.views.html.statementLanguageSelectionLayout;
import org.iatoki.judgels.sandalphon.views.html.problem.versionLocalChangesWarningLayout;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;

import java.util.List;
import java.util.Map;

public final class ProblemControllerUtils {

    public static void appendTabsLayout(LazyHtml content, Problem problem) {
        if (problem.getType() == ProblemType.PROGRAMMING) {
            ProgrammingProblemControllerUtils.appendTabsLayout(content, problem);
        }
    }

    public static void appendTitleLayout(LazyHtml content, Problem problem) {
        content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.update"), routes.ProblemController.updateProblem(problem.getId())), c));
    }

    public static void appendStatementLanguageSelectionLayout(LazyHtml content, String currentLanguage, List<String> allowedLanguages, Call target) {
        content.appendLayout(c -> statementLanguageSelectionLayout.render(target.absoluteURL(Controller.request()), allowedLanguages, currentLanguage, c));
    }

    public static void appendVersionLocalChangesWarningLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        if (problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid())) {
            content.appendLayout(c -> versionLocalChangesWarningLayout.render(problem.getId(), c));
        }
    }

    public static void establishStatementLanguage(ProblemService problemService, long problemId) {
        String currentLanguage = getCurrentStatementLanguage();
        Problem problem = problemService.findProblemById(problemId);
        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());

        if (currentLanguage == null || !availableLanguages.containsKey(currentLanguage) || availableLanguages.get(currentLanguage) == StatementLanguageStatus.DISABLED) {
            String languageCode = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());
            Controller.session("currentStatementLanguage", languageCode);
        }
    }

    public static void setCurrentStatementLanguage(String languageCode) {
        Controller.session("currentStatementLanguage", languageCode);
    }

    public static String getCurrentStatementLanguage() {
        return Controller.session("currentStatementLanguage");
    }

    public static void setJustCreatedProblem(String name, String additionalNote, String initLanguageCode) {
        Controller.session("problemName", name);
        Controller.session("problemAdditionalNote", additionalNote);
        Controller.session("initLanguageCode", initLanguageCode);
    }

    public static void removeJustCreatedProblem() {
        Controller.session().remove("problemName");
        Controller.session().remove("problemAdditionalNote");
        Controller.session().remove("initLanguageCode");
    }

    public static String getJustCreatedProblemName() {
        return Controller.session("problemName");
    }

    public static String getJustCreatedProblemAdditionalNote() {
        return Controller.session("problemAdditionalNote");
    }

    public static String getJustCreatedProblemInitLanguageCode() {
        return Controller.session("initLanguageCode");
    }

    public static boolean wasProblemJustCreated() {
        return getJustCreatedProblemName() != null &&
                getJustCreatedProblemAdditionalNote() != null &&
                getJustCreatedProblemInitLanguageCode() != null;
    }
}
