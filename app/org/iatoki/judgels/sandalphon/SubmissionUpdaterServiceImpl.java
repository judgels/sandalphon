package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.SubmissionUpdaterService;
import org.iatoki.judgels.gabriel.GradingResult;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemSubmissionModel;

public class SubmissionUpdaterServiceImpl implements SubmissionUpdaterService {

    private ProblemSubmissionDao dao;

    public SubmissionUpdaterServiceImpl(ProblemSubmissionDao dao) {
        this.dao = dao;
    }

    @Override
    public void updateResult(String submissionJid, GradingResult result) {
        ProblemSubmissionModel submissionRecord = dao.findByJid(submissionJid);
        submissionRecord.verdictCode = result.getVerdict().getCode();
        submissionRecord.verdictName = result.getVerdict().getName();
        submissionRecord.score = result.getScore();
        submissionRecord.details = result.getDetailsAsJson();

        dao.edit(submissionRecord, "Grader", "Grader's IP");
    }
}
