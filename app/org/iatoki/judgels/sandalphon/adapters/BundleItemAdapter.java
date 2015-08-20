package org.iatoki.judgels.sandalphon.adapters;

import org.iatoki.judgels.sandalphon.BundleItem;
import play.twirl.api.Html;

public interface BundleItemAdapter {

    Html renderViewHtml(BundleItem bundleItem, String conf);
}
