package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.sandalphon.BundleDetailResult;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemHasScore;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleGradingResult;
import org.iatoki.judgels.sandalphon.services.BundleProblemGrader;
import org.iatoki.judgels.sandalphon.services.ProblemService;

import java.io.IOException;
import java.util.List;

public final class BundleProblemGraderImpl implements BundleProblemGrader {

    private final ProblemService problemService;
    private final BundleItemService itemService;

    public BundleProblemGraderImpl(ProblemService problemService, BundleItemService itemService) {
        this.problemService = problemService;
        this.itemService = itemService;
    }

    @Override
    public BundleGradingResult gradeBundleProblem(String problemJid, BundleAnswer bundleAnswer) throws IOException {
        List<BundleItem> bundleItems = itemService.findAllItems(problemJid, null);
        ImmutableMap.Builder<String, BundleDetailResult> detailResultBuilder = ImmutableMap.builder();

        double totalScore = 0;
        for (BundleItem bundleItem : bundleItems) {
            String conf = "";
            try {
                conf = itemService.getItemConfByItemJid(problemJid, null, bundleItem.getJid(), bundleAnswer.getLanguageCode());
            } catch (IOException e) {
                conf = itemService.getItemConfByItemJid(problemJid, null, bundleItem.getJid(), problemService.getDefaultLanguage(null, problemJid));
            }

            BundleItemConfAdapter confAdapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            if ((adapter instanceof BundleItemHasScore) && (bundleAnswer.getAnswers().containsKey(bundleItem.getJid()))) {
                double score = ((BundleItemHasScore)adapter).calculateScore(confAdapter.parseConfString(conf), bundleAnswer.getAnswers().get(bundleItem.getJid()));
                detailResultBuilder.put(bundleItem.getJid(), new BundleDetailResult(bundleItem.getNumber(), score));
                totalScore += score;
            }
        }

        return new BundleGradingResult(totalScore, detailResultBuilder.build());
    }
}
