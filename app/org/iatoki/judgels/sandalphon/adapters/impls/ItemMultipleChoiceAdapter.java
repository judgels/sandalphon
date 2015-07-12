package org.iatoki.judgels.sandalphon.adapters.impls;

import com.google.gson.Gson;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.BundleItemConf;
import org.iatoki.judgels.sandalphon.BundleItemHasScore;
import org.iatoki.judgels.sandalphon.ItemChoice;
import org.iatoki.judgels.sandalphon.ItemMultipleChoiceConf;
import org.iatoki.judgels.sandalphon.views.html.bundle.item.itemMultipleChoiceView;
import play.twirl.api.Html;

public final class ItemMultipleChoiceAdapter implements BundleItemAdapter, BundleItemHasScore {

    @Override
    public Html renderViewHtml(BundleItem bundleItem, String conf) {
        return itemMultipleChoiceView.render(bundleItem, new Gson().fromJson(conf, ItemMultipleChoiceConf.class));
    }

    @Override
    public double calculateScore(BundleItemConf conf, String answer) {
        ItemMultipleChoiceConf realConf = (ItemMultipleChoiceConf) conf;
        for (ItemChoice itemChoice : realConf.choices) {
            if (itemChoice.getAlias().equals(answer)) {
                if (itemChoice.isCorrect()) {
                    return realConf.score;
                } else {
                    return realConf.penalty;
                }
            }
        }
        return 0;
    }
}

