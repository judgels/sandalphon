package org.iatoki.judgels.sandalphon.adapters;

import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import play.twirl.api.Html;

public interface BundleItemAdapter {
    Html renderViewHtml(BundleItem bundleItem, String conf);
}
