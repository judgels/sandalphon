package org.iatoki.judgels.sandalphon.bundle;

import com.google.common.collect.Lists;

public final class BundleItemUtils {

    private BundleItemUtils() {
        // prevent instantiation
    }

    public static BundleItemsConfig createDefaultItemConfig() {
        BundleItemsConfig itemConfig = new BundleItemsConfig();
        itemConfig.itemList = Lists.newArrayList();

        return itemConfig;
    }

}
