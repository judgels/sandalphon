package org.iatoki.judgels.sandalphon.programming;

import com.google.gson.Gson;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.commons.Submission;
import org.iatoki.judgels.commons.SubmissionAdapters;
import org.iatoki.judgels.commons.SubmissionService;
import org.iatoki.judgels.gabriel.FakeClientMessage;
import org.iatoki.judgels.gabriel.FakeSealtiel;
import org.iatoki.judgels.gabriel.GradingLanguageRegistry;
import org.iatoki.judgels.gabriel.GradingRequest;
import org.iatoki.judgels.gabriel.GradingSource;
import org.iatoki.judgels.gabriel.Verdict;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.SubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.SubmissionModel;

import java.util.List;
import java.util.stream.Collectors;

public final class SubmissionServiceImpl implements SubmissionService {
    private final SubmissionDao submissionDao;
    private final FakeSealtiel sealtiel;

    public SubmissionServiceImpl(SubmissionDao submissionDao, FakeSealtiel sealtiel) {
        this.submissionDao = submissionDao;
        this.sealtiel = sealtiel;
    }

    @Override
    public Page<Submission> pageSubmission(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = submissionDao.countByFilter(filterString);
        List<SubmissionModel> submissionRecords = submissionDao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Submission> submissions = submissionRecords
                .stream()
                .map(submissionRecord -> createSubmissionFromModel(submissionRecord))
                .collect(Collectors.toList());

        return new Page<>(submissions, totalPage, page, pageSize);
    }

    @Override
    public Submission findSubmissionById(long submissionId) {
        SubmissionModel submissionRecord = submissionDao.findById(submissionId);
        return createSubmissionFromModel(submissionRecord);
    }

    @Override
    public String submit(String problemJid, String problemGradingEngine, String gradingLanguage, long gradingLastUpdateTime, GradingSource source) {
        SubmissionModel submissionRecord = new SubmissionModel();
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

    private Submission createSubmissionFromModel(SubmissionModel record) {
        String language = GradingLanguageRegistry.getInstance().getLanguage(record.gradingLanguage).getName();
        return new Submission(record.id, record.jid, record.problemJid, record.userCreate, language, record.timeCreate, new Verdict(record.verdictCode, record.verdictName), record.score, record.details);
    }
}
