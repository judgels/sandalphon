package org.iatoki.judgels.sandalphon.bundle;

import java.util.Comparator;

public final class BundleItemOrderComparator implements Comparator<BundleItem> {

    @Override
    public int compare(BundleItem o1, BundleItem o2) {
        return Integer.compare(o1.getOrder(), o2.getOrder());
    }
}
