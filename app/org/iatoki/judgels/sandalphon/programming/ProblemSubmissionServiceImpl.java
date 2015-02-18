package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingRequest;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.gabriel.Verdict;
import org.iatoki.judgels.sandalphon.commons.SubmissionAdapters;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemSubmissionModel;
import org.iatoki.judgels.sealtiel.client.ClientMessage;
import org.iatoki.judgels.sealtiel.client.Sealtiel;
import play.Play;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class ProblemSubmissionServiceImpl implements ProblemSubmissionService {
    private final ProblemSubmissionDao submissionDao;
    private final Sealtiel sealtiel;

    public ProblemSubmissionServiceImpl(ProblemSubmissionDao submissionDao, Sealtiel sealtiel) {
        this.submissionDao = submissionDao;
        this.sealtiel = sealtiel;
    }

    @Override
    public Page<ProblemSubmission> pageSubmissions(long pageIndex, long pageSize, String orderBy, String orderDir, String authorJid, String problemJid) {
        ImmutableMap.Builder<String, String> filterColumnsBuilder = ImmutableMap.builder();
        if (authorJid != null) {
            filterColumnsBuilder.put("userCreate", authorJid);
        }
        if (problemJid != null) {
            filterColumnsBuilder.put("problemJid", problemJid);
        }

        Map<String, String> filterColumns = filterColumnsBuilder.build();

        long totalPages = submissionDao.countByFilters("", filterColumns);
        List<ProblemSubmissionModel> submissionModels = submissionDao.findSortedByFilters(orderBy, orderDir, "", filterColumns, pageIndex * pageSize, pageSize);

        List<ProblemSubmission> submissions = Lists.transform(submissionModels, m -> createSubmissionFromModel(m));

        return new Page<>(submissions, totalPages, pageIndex, pageSize);
    }

    @Override
    public ProblemSubmission findSubmissionById(long submissionId) {
        ProblemSubmissionModel submissionRecord = submissionDao.findById(submissionId);
        return createSubmissionFromModel(submissionRecord);
    }

    @Override
    public String submit(String problemJid, String problemGradingEngine, String gradingLanguage, long gradingLastUpdateTime, GradingSource source) {
        ProblemSubmissionModel submissionRecord = new ProblemSubmissionModel();
        submissionRecord.problemJid = problemJid;
        submissionRecord.gradingEngine = problemGradingEngine;
        submissionRecord.gradingLanguage = gradingLanguage;
        submissionRecord.verdictCode = "?";
        submissionRecord.verdictName = "Pending";
        submissionRecord.score = 0;
        submissionRecord.details = "";

        submissionDao.persist(submissionRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        GradingRequest request = SubmissionAdapters.fromGradingEngine(problemGradingEngine).createGradingRequest(submissionRecord.jid, problemJid, gradingLastUpdateTime, problemGradingEngine, gradingLanguage, source);

        try {
            sealtiel.sendMessage(new ClientMessage(Play.application().configuration().getString("sealtiel.gabrielClientJid"), request.getClass().getSimpleName(), new Gson().toJson(request)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return submissionRecord.jid;
    }

    private ProblemSubmission createSubmissionFromModel(ProblemSubmissionModel submissionModel) {
        String language = GradingLanguageRegistry.getInstance().getLanguage(submissionModel.gradingLanguage).getName();
        return new ProblemSubmission(submissionModel.id, submissionModel.jid, submissionModel.problemJid, submissionModel.userCreate, language, submissionModel.gradingEngine, submissionModel.timeCreate, new Verdict(submissionModel.verdictCode, submissionModel.verdictName), submissionModel.score, submissionModel.details);
    }
}
