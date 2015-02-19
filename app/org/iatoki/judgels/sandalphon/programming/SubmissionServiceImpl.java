package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.commons.AbstractSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.GradingDao;
import org.iatoki.judgels.sandalphon.programming.models.daos.interfaces.SubmissionDao;
import org.iatoki.judgels.sandalphon.programming.models.domains.GradingModel;
import org.iatoki.judgels.sandalphon.programming.models.domains.SubmissionModel;
import org.iatoki.judgels.sealtiel.client.Sealtiel;

public final class SubmissionServiceImpl extends AbstractSubmissionServiceImpl<SubmissionModel, GradingModel> {
    public SubmissionServiceImpl(SubmissionDao submissionDao, GradingDao gradingDao, Sealtiel sealtiel, String gabrielClientJid) {
        super(submissionDao, gradingDao, sealtiel, gabrielClientJid);
    }
}
