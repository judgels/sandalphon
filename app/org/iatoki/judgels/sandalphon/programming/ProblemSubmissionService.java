package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.GradingSource;

public interface ProblemSubmissionService {
    Page<ProblemSubmission> pageSubmission(long page, long pageSize, String sortBy, String order, String filterString);

    ProblemSubmission findSubmissionById(long submissionId);

    String submit(String problemJid, String problemGradingType, String gradingLanguage, long problemTimeUpdate, GradingSource source);
}
