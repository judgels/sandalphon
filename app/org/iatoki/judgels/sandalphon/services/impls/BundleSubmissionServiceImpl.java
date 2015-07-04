package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.services.BundleProblemGrader;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleGradingModel;
import org.iatoki.judgels.sandalphon.models.entities.bundle.BundleSubmissionModel;
import org.iatoki.judgels.sandalphon.services.BundleSubmissionService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("bundleSubmissionService")
public final class BundleSubmissionServiceImpl extends AbstractBundleSubmissionServiceImpl<BundleSubmissionModel, BundleGradingModel> implements BundleSubmissionService {

    @Inject
    public BundleSubmissionServiceImpl(BundleSubmissionDao bundleSubmissionDao, BundleGradingDao bundleGradingDao, BundleProblemGrader bundleProblemGrader) {
        super(bundleSubmissionDao, bundleGradingDao, bundleProblemGrader);
    }
}
