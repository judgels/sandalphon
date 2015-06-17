package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.JidService;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.bundle.BundleItem;
import org.iatoki.judgels.sandalphon.bundle.BundleItemAdapters;
import org.iatoki.judgels.sandalphon.bundle.BundleItemComparator;
import org.iatoki.judgels.sandalphon.bundle.BundleItemHasScore;
import org.iatoki.judgels.sandalphon.bundle.BundleItemType;
import org.iatoki.judgels.sandalphon.bundle.BundleItemsConfig;
import org.iatoki.judgels.sandalphon.services.BundleItemService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class BundleItemServiceImpl implements BundleItemService {

    private final FileSystemProvider fileSystemProvider;

    public BundleItemServiceImpl(FileSystemProvider fileSystemProvider) {
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public boolean existByItemJid(String problemJid, String userJid, String itemJid) throws IOException {
        return fileSystemProvider.directoryExists(BundleServiceUtils.getItemDirPath(fileSystemProvider, problemJid, userJid, itemJid));
    }

    @Override
    public boolean existByMeta(String problemJid, String userJid, String meta) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);

        for (BundleItem bundleItem : bundleItemsConfig.itemList) {
            if (bundleItem.getMeta().equals(meta)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public BundleItem findByItemJid(String problemJid, String userJid, String itemJid) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);

        for (BundleItem bundleItem : bundleItemsConfig.itemList) {
            if (bundleItem.getJid().equals(itemJid)) {
                return bundleItem;
            }
        }

        return null;
    }

    @Override
    public String getItemConfByItemJid(String problemJid, String userJid, String itemJid, String languageCode) throws IOException {
        return fileSystemProvider.readFromFile(BundleServiceUtils.getItemConfigFilePath(fileSystemProvider, problemJid, userJid, itemJid, languageCode));
    }

    @Override
    public Page<BundleItem> pageItems(String problemJid, String userJid, long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = bundleItemsConfig.itemList;

        List<BundleItem> filteredBundleItems = bundleItems.stream()
              .filter(b -> (StringUtils.containsIgnoreCase(b.getMeta(), filterString)) || StringUtils.containsIgnoreCase(b.getJid(), filterString) || StringUtils.containsIgnoreCase(b.getType().name(), filterString))
              .sorted(new BundleItemComparator(orderBy, orderDir))
              .skip(pageIndex * pageSize)
              .limit(pageSize)
              .collect(Collectors.toList());

        long number = 1;
        for (BundleItem bundleItem : filteredBundleItems) {
            if (BundleItemAdapters.fromItemType(bundleItem.getType()) instanceof BundleItemHasScore) {
                bundleItem.setNumber(number++);
            }
        }

        return new Page<>(filteredBundleItems, bundleItems.size(), pageIndex, pageSize);
    }

    @Override
    public List<BundleItem> findAllItems(String problemJid, String userJid) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);

        List<BundleItem> bundleItems = bundleItemsConfig.itemList.stream().collect(Collectors.toList());
        long number = 1;
        for (BundleItem bundleItem : bundleItems) {
            if (BundleItemAdapters.fromItemType(bundleItem.getType()) instanceof BundleItemHasScore) {
                bundleItem.setNumber(number++);
            }
        }

        return bundleItems;
    }

    @Override
    public void createItem(String problemJid, String userJid, BundleItemType itemType, String meta, String conf, String languageCode) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = Lists.newArrayList(bundleItemsConfig.itemList);

        String itemJid = JidService.getInstance().generateNewJid("ITEM");
        BundleItem bundleItem = new BundleItem(itemJid, itemType, meta);
        bundleItems.add(bundleItem);

        bundleItemsConfig.itemList = bundleItems;
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid), new Gson().toJson(bundleItemsConfig));

        fileSystemProvider.createDirectory(BundleServiceUtils.getItemDirPath(fileSystemProvider, problemJid, userJid, itemJid));
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemConfigFilePath(fileSystemProvider, problemJid, userJid, itemJid, languageCode), conf);
    }

    @Override
    public void updateItem(String problemJid, String userJid, String itemJid, String meta, String conf, String languageCode) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = Lists.newArrayList(bundleItemsConfig.itemList);

        int i = 0;
        if (bundleItems.size() > 0) {
            do {
                if (bundleItems.get(i).getJid().equals(itemJid)) {
                    BundleItem current = bundleItems.get(i);
                    bundleItems.set(i, new BundleItem(current.getJid(), current.getType(), meta));
                }
                ++i;
            } while ((i < bundleItems.size()) && (!bundleItems.get(i - 1).getJid().equals(itemJid)));
        }

        bundleItemsConfig.itemList = bundleItems;
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid), new Gson().toJson(bundleItemsConfig));

        fileSystemProvider.writeToFile(BundleServiceUtils.getItemConfigFilePath(fileSystemProvider, problemJid, userJid, itemJid, languageCode), conf);
    }

    @Override
    public void moveItemUp(String problemJid, String userJid, String itemJid) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = Lists.newArrayList(bundleItemsConfig.itemList);

        int i = 1;
        if (bundleItems.size() > 0) {
            do {
                if (bundleItems.get(i).getJid().equals(itemJid)) {
                    BundleItem current = bundleItems.get(i);
                    BundleItem previous = bundleItems.get(i - 1);
                    bundleItems.set(i, new BundleItem(previous.getJid(), previous.getType(), previous.getMeta()));
                    bundleItems.set(i - 1, new BundleItem(current.getJid(), current.getType(), current.getMeta()));
                }
                ++i;
            } while ((i < bundleItems.size()) && (!bundleItems.get(i - 1).getJid().equals(itemJid)));
        }

        bundleItemsConfig.itemList = bundleItems;
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid), new Gson().toJson(bundleItemsConfig));
    }

    @Override
    public void moveItemDown(String problemJid, String userJid, String itemJid) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = Lists.newArrayList(bundleItemsConfig.itemList);

        int i = 0;
        if (bundleItems.size() > 0) {
            do {
                if (bundleItems.get(i).getJid().equals(itemJid)) {
                    BundleItem current = bundleItems.get(i);
                    BundleItem next = bundleItems.get(i + 1);
                    bundleItems.set(i, new BundleItem(next.getJid(), next.getType(), next.getMeta()));
                    bundleItems.set(i + 1, new BundleItem(current.getJid(), current.getType(), current.getMeta()));
                }
                ++i;
            } while ((i < bundleItems.size() - 1) && (!bundleItems.get(i - 1).getJid().equals(itemJid)));
        }

        bundleItemsConfig.itemList = bundleItems;
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid), new Gson().toJson(bundleItemsConfig));
    }

    @Override
    public void removeItem(String problemJid, String userJid, String itemJid) throws IOException {
        List<String> itemsConfig = BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid);
        BundleItemsConfig bundleItemsConfig = new Gson().fromJson(fileSystemProvider.readFromFile(itemsConfig), BundleItemsConfig.class);
        List<BundleItem> bundleItems = Lists.newArrayList(bundleItemsConfig.itemList);

        int toBeRemovedIndex = -1;
        int i = 0;
        if (bundleItems.size() > 0) {
            do {
                if (bundleItems.get(i).getJid().equals(itemJid)) {
                    toBeRemovedIndex = i;
                }
                ++i;
            } while ((i < bundleItems.size()) && (!bundleItems.get(i - 1).getJid().equals(itemJid)));

            if (toBeRemovedIndex != -1) {
                bundleItems.remove(toBeRemovedIndex);
            }
        }

        bundleItemsConfig.itemList = bundleItems;
        fileSystemProvider.writeToFile(BundleServiceUtils.getItemsConfigFilePath(fileSystemProvider, problemJid, userJid), new Gson().toJson(bundleItemsConfig));
    }
}
