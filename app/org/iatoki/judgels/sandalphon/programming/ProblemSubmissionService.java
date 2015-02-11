package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingSource;

public interface ProblemSubmissionService {
    Page<ProblemSubmission> pageSubmissions(long pageIndex, long pageSize, String orderBy, String orderDir, String authorJid, String problemJid);

    ProblemSubmission findSubmissionById(long submissionId);

    String submit(String problemJid, String problemGradingType, String gradingLanguage, long problemTimeUpdate, GradingSource source);
}
