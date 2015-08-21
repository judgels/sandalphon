package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;

import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import org.iatoki.judgels.sandalphon.views.html.problem.version.versionLocalChangesWarningLayout;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProblemControllerUtils {

    private ProblemControllerUtils() {
        // prevent instantiation
    }

    static void appendTabsLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        if (problem.getType().equals(ProblemType.PROGRAMMING)) {
            ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        } else if (problem.getType().equals(ProblemType.BUNDLE)) {
            BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        }
    }

    static void appendTitleLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        if (isAllowedToUpdateProblem(problemService, problem)) {
            content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.update"), routes.ProblemController.updateProblem(problem.getId())), c));
        } else {
            content.appendLayout(c -> headingWithActionLayout.render("#" + problem.getId() + ": " + problem.getName(), new InternalLink(Messages.get("problem.view"), routes.ProblemController.viewProblem(problem.getId())), c));
        }
    }

    static void appendStatementLanguageSelectionLayout(LazyHtml content, String currentLanguage, Set<String> allowedLanguages, Call target) {
        content.appendLayout(c -> statementLanguageSelectionLayout.render(target.absoluteURL(Controller.request(), Controller.request().secure()), allowedLanguages, currentLanguage, c));
    }

    static void appendVersionLocalChangesWarningLayout(LazyHtml content, ProblemService problemService, Problem problem) {
        if (problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid())) {
            content.appendLayout(c -> versionLocalChangesWarningLayout.render(problem.getId(), c));
        }
    }

    static void establishStatementLanguage(ProblemService problemService, Problem problem) throws IOException {
        String currentLanguage = getCurrentStatementLanguage();
        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());

        if (currentLanguage == null || !availableLanguages.containsKey(currentLanguage) || availableLanguages.get(currentLanguage) == StatementLanguageStatus.DISABLED) {
            String languageCode = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());
            setCurrentStatementLanguage(languageCode);
        }
    }

    static String getDefaultStatementLanguage(ProblemService problemService, Problem problem) throws IOException {
        return problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());
    }

    static void setCurrentStatementLanguage(String languageCode) {
        Controller.session("currentStatementLanguage", languageCode);
    }

    static String getCurrentStatementLanguage() {
        return Controller.session("currentStatementLanguage");
    }

    static void setJustCreatedProblem(String name, String additionalNote, String initLanguageCode) {
        Controller.session("problemName", name);
        Controller.session("problemAdditionalNote", additionalNote);
        Controller.session("initLanguageCode", initLanguageCode);
    }

    static void removeJustCreatedProblem() {
        Controller.session().remove("problemName");
        Controller.session().remove("problemAdditionalNote");
        Controller.session().remove("initLanguageCode");
    }

    static String getJustCreatedProblemName() {
        return Controller.session("problemName");
    }

    static String getJustCreatedProblemAdditionalNote() {
        return Controller.session("problemAdditionalNote");
    }

    static String getJustCreatedProblemInitLanguageCode() {
        return Controller.session("initLanguageCode");
    }

    static boolean wasProblemJustCreated() {
        return getJustCreatedProblemName() != null
                && getJustCreatedProblemAdditionalNote() != null
                && getJustCreatedProblemInitLanguageCode() != null;
    }

    static ImmutableList.Builder<InternalLink> getProblemBreadcrumbsBuilder(Problem problem) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks
                .add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()))
                .add(new InternalLink(problem.getName(), routes.ProblemController.enterProblem(problem.getId())));

        return internalLinks;
    }

    static Result downloadFile(File file) {
        if (!file.exists()) {
            return Results.notFound();
        }
        Controller.response().setContentType("application/x-download");
        Controller.response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
        return Results.ok(file);
    }

    static boolean isAuthor(Problem problem) {
        return problem.getAuthorJid().equals(IdentityUtils.getUserJid());
    }

    static boolean isAuthorOrAbove(Problem problem) {
        return ControllerUtils.getInstance().isAdmin() || isAuthor(problem);
    }

    static boolean isPartner(ProblemService problemService, Problem problem) {
        return problemService.isUserPartnerForProblem(problem.getJid(), IdentityUtils.getUserJid());
    }

    static boolean isPartnerOrAbove(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || isPartner(problemService, problem);
    }

    static boolean isAllowedToUpdateProblem(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToUpdateProblem());
    }

    static boolean isAllowedToUploadStatementResources(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToUploadStatementResources());
    }

    static boolean isAllowedToViewStatement(ProblemService problemService, Problem problem) {
        if (isAuthorOrAbove(problem)) {
            return true;
        }

        if (!isPartner(problemService, problem)) {
            return false;
        }

        String language = getCurrentStatementLanguage();

        try {
            String defaultLanguage = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());
            Set<String> allowedLanguages = getPartnerConfig(problemService, problem).getAllowedStatementLanguagesToView();

            if (allowedLanguages == null || allowedLanguages.contains(language) || language.equals(defaultLanguage)) {
                return true;
            }

            setCurrentStatementLanguage(defaultLanguage);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    static boolean isAllowedToUpdateStatement(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToUpdateStatement());
    }

    static boolean isAllowedToUpdateStatementInLanguage(ProblemService problemService, Problem problem) {
        if (!isAllowedToUpdateStatement(problemService, problem)) {
            return false;
        }

        if (isAuthorOrAbove(problem)) {
            return true;
        }

        if (!isPartner(problemService, problem)) {
            return false;
        }

        String language = getCurrentStatementLanguage();

        Set<String> allowedLanguages = getPartnerConfig(problemService, problem).getAllowedStatementLanguagesToUpdate();

        if (allowedLanguages == null || allowedLanguages.contains(language)) {
            return true;
        }

        String firstLanguage = allowedLanguages.iterator().next();

        setCurrentStatementLanguage(firstLanguage);
        return true;
    }

    static boolean isAllowedToManageStatementLanguages(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToManageStatementLanguages());
    }

    static boolean isAllowedToViewVersionHistory(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToViewVersionHistory());
    }

    static boolean isAllowedToRestoreVersionHistory(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToRestoreVersionHistory());
    }

    static boolean isAllowedToManageClients(ProblemService problemService, Problem problem) {
        return isAuthorOrAbove(problem) || (isPartner(problemService, problem) && getPartnerConfig(problemService, problem).isAllowedToManageProblemClients());
    }

    static ProblemPartnerConfig getPartnerConfig(ProblemService problemService, Problem problem) {
        return problemService.findProblemPartnerByProblemJidAndPartnerJid(problem.getJid(), IdentityUtils.getUserJid()).getBaseConfig();
    }

    static Set<String> getAllowedLanguagesToView(ProblemService problemService, Problem problem) throws IOException {
        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());

        Set<String> allowedLanguages = Sets.newTreeSet();
        allowedLanguages.addAll(availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet()));

        if (isPartner(problemService, problem)) {
            Set<String> allowedPartnerLanguages = getPartnerConfig(problemService, problem).getAllowedStatementLanguagesToView();
            if (allowedPartnerLanguages != null) {
                allowedLanguages.retainAll(allowedPartnerLanguages);
                allowedLanguages.add(problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid()));
            }
        }

        return ImmutableSet.copyOf(allowedLanguages);
    }

    static Set<String> getAllowedLanguagesToUpdate(ProblemService problemService, Problem problem) throws IOException {
        Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());

        Set<String> allowedLanguages = Sets.newTreeSet();
        allowedLanguages.addAll(availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet()));

        if (isPartner(problemService, problem) && isAllowedToUpdateStatement(problemService, problem)) {
            Set<String> allowedPartnerLanguages = getPartnerConfig(problemService, problem).getAllowedStatementLanguagesToUpdate();
            if (allowedPartnerLanguages != null) {
                allowedLanguages.retainAll(allowedPartnerLanguages);
            }
        }

        return ImmutableSet.copyOf(allowedLanguages);
    }
}
