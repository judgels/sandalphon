package org.iatoki.judgels.sandalphon.bundle;

import org.iatoki.judgels.sandalphon.commons.AbstractBundleSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.commons.BundleProblemGrader;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.bundle.BundleGradingModel;
import org.iatoki.judgels.sandalphon.models.domains.bundle.BundleSubmissionModel;

public final class BundleSubmissionServiceImpl extends AbstractBundleSubmissionServiceImpl<BundleSubmissionModel, BundleGradingModel> {
    public BundleSubmissionServiceImpl(BundleSubmissionDao submissionDao, BundleGradingDao gradingDao, BundleProblemGrader bundleProblemGrader) {
        super(submissionDao, gradingDao, bundleProblemGrader);
    }
}
