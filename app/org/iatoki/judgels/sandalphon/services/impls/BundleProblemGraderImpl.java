package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleDetailResult;
import org.iatoki.judgels.sandalphon.BundleGradingResult;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.adapters.BundleItemConfAdapter;
import org.iatoki.judgels.sandalphon.adapters.impls.BundleItemConfAdapters;
import org.iatoki.judgels.sandalphon.BundleItemHasScore;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.services.BundleProblemGrader;
import org.iatoki.judgels.sandalphon.services.ProblemService;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.List;

@Named("bundleProblemGrader")
public final class BundleProblemGraderImpl implements BundleProblemGrader {

    private final BundleItemService bundleItemService;
    private final ProblemService problemService;

    @Inject
    public BundleProblemGraderImpl(BundleItemService bundleItemService, ProblemService problemService) {
        this.bundleItemService = bundleItemService;
        this.problemService = problemService;
    }

    @Override
    public BundleGradingResult gradeBundleProblem(String problemJid, BundleAnswer answer) throws IOException {
        List<BundleItem> bundleItems = bundleItemService.getBundleItemsInProblemWithClone(problemJid, null);
        ImmutableMap.Builder<String, BundleDetailResult> detailResultBuilder = ImmutableMap.builder();

        double totalScore = 0;
        for (BundleItem bundleItem : bundleItems) {
            String conf = "";
            try {
                conf = bundleItemService.getItemConfInProblemWithCloneByJid(problemJid, null, bundleItem.getJid(), answer.getLanguageCode());
            } catch (IOException e) {
                conf = bundleItemService.getItemConfInProblemWithCloneByJid(problemJid, null, bundleItem.getJid(), problemService.getDefaultLanguage(null, problemJid));
            }

            BundleItemConfAdapter confAdapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            if ((adapter instanceof BundleItemHasScore) && answer.getAnswers().containsKey(bundleItem.getJid())) {
                double score = ((BundleItemHasScore) adapter).calculateScore(confAdapter.parseConfString(conf), answer.getAnswers().get(bundleItem.getJid()));
                detailResultBuilder.put(bundleItem.getJid(), new BundleDetailResult(bundleItem.getNumber(), score));
                totalScore += score;
            }
        }

        return new BundleGradingResult(totalScore, detailResultBuilder.build());
    }
}
