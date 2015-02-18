package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingSource;

import java.util.Date;

public interface ProblemSubmissionService {
    Page<ProblemSubmission> pageSubmissions(long pageIndex, long pageSize, String orderBy, String orderDir, String authorJid, String problemJid);

    ProblemSubmission findSubmissionById(long submissionId);

    String submit(String problemJid, String gradingEngine, String gradingLanguage, Date gradingLastUpdateTime, GradingSource source);
}
