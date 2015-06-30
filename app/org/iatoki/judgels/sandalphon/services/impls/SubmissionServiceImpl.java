package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.models.daos.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.GradingModel;
import org.iatoki.judgels.sandalphon.models.entities.programming.ProgrammingSubmissionModel;
import org.iatoki.judgels.sealtiel.Sealtiel;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("submissionService")
public final class SubmissionServiceImpl extends AbstractSubmissionServiceImpl<ProgrammingSubmissionModel, GradingModel> {

    @Inject
    public SubmissionServiceImpl(ProgrammingSubmissionDao submissionDao, GradingDao gradingDao, Sealtiel sealtiel, String gabrielClientJid) {
        super(submissionDao, gradingDao, sealtiel, gabrielClientJid);
    }
}
