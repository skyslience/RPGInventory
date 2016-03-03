package ru.endlesscode.rpginventory.inventory.backpack;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.endlesscode.rpginventory.RPGInventory;
import ru.endlesscode.rpginventory.utils.ItemUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by OsipXD on 05.10.2015
 * It is part of the RpgInventory.
 * All rights reserved 2014 - 2015 © «EndlessCode Group»
 */
public class BackpackManager {
    private static final HashMap<String, BackpackType> BACKPACK_TYPES = new HashMap<>();
    private static final HashMap<UUID, Backpack> BACKPACKS = new HashMap<>();

    public static void init() {
        File petsFile = new File(RPGInventory.getInstance().getDataFolder(), "backpacks.yml");
        if (!petsFile.exists()) {
            RPGInventory.getInstance().saveResource("backpacks.yml", false);
        }

        FileConfiguration petsConfig = YamlConfiguration.loadConfiguration(petsFile);

        BACKPACK_TYPES.clear();
        for (String key : petsConfig.getConfigurationSection("backpacks").getKeys(false)) {
            BackpackType backpackType = new BackpackType(petsConfig.getConfigurationSection("backpacks." + key));
            BACKPACK_TYPES.put(key, backpackType);
        }

        BackpackManager.loadBackpacks();
        RPGInventory.getInstance().getLogger().info(BACKPACK_TYPES.size() + " backpack type(s) has been loaded");
        RPGInventory.getInstance().getLogger().info(BACKPACKS.size() + " backpack(s) has been loaded");
    }

    public static List<String> getBackpackList() {
        List<String> backpackList = new ArrayList<>();
        backpackList.addAll(BACKPACK_TYPES.keySet());
        return backpackList;
    }

    public static ItemStack getItem(String id) {
        BackpackType backpackType = BACKPACK_TYPES.get(id);
        return backpackType == null ? new ItemStack(Material.AIR) : backpackType.getItem();
    }

    @Contract("_, null -> false")
    public static boolean open(@NotNull Player player, @Nullable ItemStack bpItem) {
        if (bpItem == null || bpItem.getType() == Material.AIR) {
            return false;
        }

        BackpackType type;
        String bpId = ItemUtils.getTag(bpItem, ItemUtils.BACKPACK_TAG);

        if (bpId == null || (type = BackpackManager.getBackpackType(bpId)) == null) {
            return false;
        }

        Backpack backpack;
        String bpUniqueId = ItemUtils.getTag(bpItem, ItemUtils.BACKPACK_UID_TAG);
        if (bpUniqueId == null || !BACKPACKS.containsKey(UUID.fromString(bpUniqueId))) {
            backpack = type.createBackpack();
            ItemUtils.setTag(bpItem, ItemUtils.BACKPACK_UID_TAG, backpack.getUniqueId().toString());
            BACKPACKS.put(backpack.getUniqueId(), backpack);
        } else {
            backpack = BACKPACKS.get(UUID.fromString(bpUniqueId));
        }

        backpack.open(player);
        return true;
    }

    @Nullable
    public static BackpackType getBackpackType(String bpId) {
        return BACKPACK_TYPES.get(bpId);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void saveBackpacks() {
        File folder = new File(RPGInventory.getInstance().getDataFolder(), "backpacks");
        if (!folder.exists()) {
            folder.mkdir();
        }

        try {
            for (Map.Entry<UUID, Backpack> entry : BACKPACKS.entrySet()) {
                File bpFile = new File(folder, entry.getKey().toString() + ".bp");
                BackpackSerializer.saveBackpack(entry.getValue(), bpFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void loadBackpacks() {
        try {
            File folder = new File(RPGInventory.getInstance().getDataFolder(), "backpacks");
            if (!folder.exists()) {
                folder.mkdir();
            }

            //noinspection ConstantConditions
            for (File bpFile : folder.listFiles()) {
                if (bpFile.getName().endsWith(".bp")) {
                    Backpack backpack = BackpackSerializer.loadBackpack(bpFile);
                    if (backpack == null || backpack.isOverdue()) {
                        bpFile.delete();
                        continue;
                    }

                    BACKPACKS.put(backpack.getUniqueId(), backpack);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Contract("null -> false")
    public static boolean isBackpack(ItemStack item) {
        return item != null && item.getType() != Material.AIR && ItemUtils.hasTag(item, ItemUtils.BACKPACK_TAG);
    }
}