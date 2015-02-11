package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.sandalphon.commons.AbstractSubmissionUpdaterServiceImpl;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.ProblemSubmissionDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.ProblemSubmissionModel;

public final class SubmissionUpdaterServiceImpl extends AbstractSubmissionUpdaterServiceImpl<ProblemSubmissionModel> {
    public SubmissionUpdaterServiceImpl(ProblemSubmissionDao submissionDao) {
        super(submissionDao);
    }
}
