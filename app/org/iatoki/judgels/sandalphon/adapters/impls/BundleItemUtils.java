package org.iatoki.judgels.sandalphon.adapters.impls;

import com.google.common.collect.Lists;
import org.iatoki.judgels.sandalphon.BundleItemsConfig;

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
