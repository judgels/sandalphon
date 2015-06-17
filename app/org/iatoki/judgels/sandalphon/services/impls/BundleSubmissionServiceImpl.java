package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.commons.AbstractBundleSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.commons.BundleProblemGrader;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleGradingModel;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleSubmissionModel;

public final class BundleSubmissionServiceImpl extends AbstractBundleSubmissionServiceImpl<BundleSubmissionModel, BundleGradingModel> {
    public BundleSubmissionServiceImpl(BundleSubmissionDao submissionDao, BundleGradingDao gradingDao, BundleProblemGrader bundleProblemGrader) {
        super(submissionDao, gradingDao, bundleProblemGrader);
    }
}
