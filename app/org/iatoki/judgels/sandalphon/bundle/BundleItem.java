package org.iatoki.judgels.sandalphon.bundle;

public final class BundleItem {

    private final int order;
    private final String jid;
    private final BundleItemType type;
    private final String meta;

    public BundleItem(int order, String jid, BundleItemType type, String meta) {
        this.order = order;
        this.jid = jid;
        this.type = type;
        this.meta = meta;
    }

    public int getOrder() {
        return order;
    }

    public String getJid() {
        return jid;
    }

    public BundleItemType getType() {
        return type;
    }

    public String getMeta() {
        return meta;
    }
}
