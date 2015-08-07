package org.iatoki.judgels.sandalphon.adapters.impls;

import org.iatoki.judgels.sandalphon.adapters.BundleItemAdapter;
import org.iatoki.judgels.sandalphon.BundleItemType;

public final class BundleItemAdapters {
    private BundleItemAdapters() {
        // prevent instantiation
    }

    public static BundleItemAdapter fromItemType(BundleItemType itemType) {
        BundleItemAdapter itemAdapter = null;

        switch (itemType) {
            case STATEMENT:
                itemAdapter = new ItemStatementAdapter();
                break;
            case MULTIPLE_CHOICE:
                itemAdapter = new ItemMultipleChoiceAdapter();
                break;
            default: break;
        }

        return itemAdapter;
    }
}
