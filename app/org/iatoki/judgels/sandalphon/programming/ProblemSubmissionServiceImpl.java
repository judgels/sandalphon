package org.iatoki.judgels.sandalphon.programming;

import com.google.gson.Gson;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.SubmissionAdapters;
import org.iatoki.judgels.gabriel.FakeClientMessage;
import org.iatoki.judgels.gabriel.FakeSealtiel;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingRequest;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.gabriel.Verdict;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemSubmissionModel;

import java.util.List;
import java.util.stream.Collectors;

public final class ProblemSubmissionServiceImpl implements ProblemSubmissionService {
    private final ProblemSubmissionDao submissionDao;
    private final FakeSealtiel sealtiel;

    public ProblemSubmissionServiceImpl(ProblemSubmissionDao submissionDao, FakeSealtiel sealtiel) {
        this.submissionDao = submissionDao;
        this.sealtiel = sealtiel;
    }

    @Override
    public Page<ProblemSubmission> pageSubmission(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = submissionDao.countByFilter(filterString);
        List<ProblemSubmissionModel> submissionRecords = submissionDao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<ProblemSubmission> submissions = submissionRecords
                .stream()
                .map(submissionRecord -> createSubmissionFromModel(submissionRecord))
                .collect(Collectors.toList());

        return new Page<>(submissions, totalPage, page, pageSize);
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
        submissionRecord.gradingLanguage = gradingLanguage;
        submissionRecord.verdictCode = "?";
        submissionRecord.verdictName = "Pending";
        submissionRecord.score = 0;
        submissionRecord.details = "";

        submissionDao.persist(submissionRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        GradingRequest request = SubmissionAdapters.fromGradingEngine(problemGradingEngine).createGradingRequest(submissionRecord.jid, problemJid, gradingLastUpdateTime, problemGradingEngine, gradingLanguage, source);

        FakeClientMessage message = new FakeClientMessage("some-target", request.getClass().getSimpleName(), new Gson().toJson(request));
        sealtiel.sendMessage(message);

        return submissionRecord.jid;
    }

    private ProblemSubmission createSubmissionFromModel(ProblemSubmissionModel record) {
        String language = GradingLanguageRegistry.getInstance().getLanguage(record.gradingLanguage).getName();
        return new ProblemSubmission(record.id, record.jid, record.problemJid, record.userCreate, language, record.timeCreate, new Verdict(record.verdictCode, record.verdictName), record.score, record.details);
    }
}
