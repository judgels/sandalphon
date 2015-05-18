package org.iatoki.judgels.sandalphon.bundle;

import play.twirl.api.Html;

public interface BundleItemAdapter {
    Html renderViewHtml(BundleItem bundleItem, String conf);
}
