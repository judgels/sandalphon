package org.iatoki.judgels.sandalphon.bundle;

import java.util.Comparator;

public final class BundleItemComparator implements Comparator<BundleItem> {

    private final String orderBy;
    private final String orderDir;

    public BundleItemComparator(String orderBy, String orderDir) {
        this.orderBy = orderBy;
        this.orderDir = orderDir;
    }

    @Override
    public int compare(BundleItem o1, BundleItem o2) {
        if (orderDir.equals("asc")) {
            BundleItem temp = o2;
            o2 = o1;
            o1 = temp;
        }
        switch (orderBy) {
            case "jid": {
                return o1.getJid().compareTo(o2.getJid());
            }
            case "type": {
                return o1.getType().compareTo(o2.getType());
            }
            case "meta": {
                return o1.getMeta().compareTo(o2.getMeta());
            }
            default: break;
        }
        return 0;
    }
}
