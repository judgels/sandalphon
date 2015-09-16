package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.api.sealtiel.SealtielClientAPI;
import org.iatoki.judgels.sandalphon.config.GabrielClientJid;
import org.iatoki.judgels.sandalphon.models.daos.ProgrammingGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.ProgrammingGradingModel;
import org.iatoki.judgels.sandalphon.models.entities.ProgrammingSubmissionModel;
import org.iatoki.judgels.sandalphon.services.ProgrammingSubmissionService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("programmingSubmissionService")
public final class ProgrammingSubmissionServiceImpl extends AbstractProgrammingSubmissionServiceImpl<ProgrammingSubmissionModel, ProgrammingGradingModel> implements ProgrammingSubmissionService {

    @Inject
    public ProgrammingSubmissionServiceImpl(ProgrammingSubmissionDao submissionDao, ProgrammingGradingDao programmingGradingDao, SealtielClientAPI sealtielClientAPI, @GabrielClientJid String gabrielClientJid) {
        super(submissionDao, programmingGradingDao, sealtielClientAPI, gabrielClientJid);
    }
}
