package org.iatoki.judgels.sandalphon.adapters.impls;

import com.google.gson.Gson;
import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.BundleItem;
import org.iatoki.judgels.sandalphon.ItemStatementConf;
import org.iatoki.judgels.sandalphon.views.html.bundle.item.itemStatementView;
import play.twirl.api.Html;

public final class ItemStatementAdapter implements BundleItemAdapter {

    @Override
    public Html renderViewHtml(BundleItem bundleItem, String conf) {
        return itemStatementView.render(bundleItem, new Gson().fromJson(conf, ItemStatementConf.class));
    }
}
