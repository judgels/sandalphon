package org.iatoki.judgels.sandalphon.bundle;

import org.iatoki.judgels.commons.Page;

import java.io.IOException;
import java.util.List;

public interface BundleItemService {

    boolean existByItemJid(String problemJid, String userJid, String itemJid) throws IOException;

    BundleItem findByItemJid(String problemJid, String userJid, String itemJid) throws IOException;

    String getItemConfByItemJid(String problemJid, String userJid, String itemJid, String languageCode) throws IOException;

    Page<BundleItem> pageItems(String problemJid, String userJid, long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) throws IOException;

    List<BundleItem> findAllItems(String problemJid, String userJid) throws IOException;

    void createItem(String problemJid, String userJid, BundleItemType itemType, String meta, String conf, String languageCode) throws IOException;

    void updateItem(String problemJid, String userJid, String itemJid, String meta, String conf, String languageCode) throws IOException;

    void moveItemUp(String problemJid, String userJid, String itemJid) throws IOException;

    void moveItemDown(String problemJid, String userJid, String itemJid) throws IOException;
}
