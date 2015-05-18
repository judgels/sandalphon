package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapter;
import org.iatoki.judgels.sandalphon.bundle.BundleItemConfAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemHasScore;
import org.iatoki.judgels.sandalphon.bundle.BundleItemService;
import org.iatoki.judgels.sandalphon.commons.BundleAnswer;
import org.iatoki.judgels.sandalphon.commons.BundleGradingResult;
import org.iatoki.judgels.sandalphon.commons.BundleProblemGrader;

import java.io.IOException;
import java.util.List;

public final class BundleProblemGraderImpl implements BundleProblemGrader {

    private final BundleItemService itemService;

    public BundleProblemGraderImpl(BundleItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public BundleGradingResult gradeBundleProblem(String problemJid, BundleAnswer bundleAnswer) throws IOException {
        List<BundleItem> bundleItems = itemService.findAllItems(problemJid, null);
        ImmutableMap.Builder<String, Double> detailResultBuilder = ImmutableMap.builder();

        double totalScore = 0;
        for (BundleItem bundleItem : bundleItems) {
            String conf = itemService.getItemConfByItemJid(problemJid, null, bundleItem.getJid(), bundleAnswer.getLanguageCode());

            BundleItemConfAdapter confAdapter = BundleItemConfAdapters.fromItemType(bundleItem.getType());
            BundleItemAdapter adapter = BundleItemAdapters.fromItemType(bundleItem.getType());
            if ((adapter instanceof BundleItemHasScore) && (bundleAnswer.getAnswers().containsKey(bundleItem.getJid()))) {
                double score = ((BundleItemHasScore)adapter).calculateScore(confAdapter.parseConfString(conf), bundleAnswer.getAnswers().get(bundleItem.getJid()));
                detailResultBuilder.put(bundleItem.getJid(), score);
                totalScore += score;
            }
        }

        return new BundleGradingResult(totalScore, detailResultBuilder.build());
    }
}
