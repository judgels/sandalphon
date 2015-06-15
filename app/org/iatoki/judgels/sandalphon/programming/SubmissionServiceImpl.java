package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.sandalphon.commons.AbstractSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.GradingModel;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProgrammingSubmissionModel;
import org.iatoki.judgels.sealtiel.Sealtiel;

public final class SubmissionServiceImpl extends AbstractSubmissionServiceImpl<ProgrammingSubmissionModel, GradingModel> {
    public SubmissionServiceImpl(ProgrammingSubmissionDao submissionDao, GradingDao gradingDao, Sealtiel sealtiel, String gabrielClientJid) {
        super(submissionDao, gradingDao, sealtiel, gabrielClientJid);
    }
}
