package net.eltown.servercore.components.api;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.*;

public class ServerCoreAPI {

    private static HologramAPI hologramAPI;
    private static LevelAPI levelAPI;
    private static GroupAPI groupAPI;
    private static ChestShopAPI chestShopAPI;
    private static QuestAPI questAPI;
    private static SettingsAPI settingsAPI;
    private static CrateAPI crateAPI;

    public ServerCoreAPI(final ServerCore serverCore) {
        hologramAPI = serverCore.getHologramAPI();
        levelAPI = serverCore.getLevelAPI();
        groupAPI = serverCore.getGroupAPI();
        chestShopAPI = serverCore.getChestShopAPI();
        questAPI = serverCore.getQuestAPI();
        settingsAPI = serverCore.getSettingsAPI();
        crateAPI = serverCore.getCrateAPI();
    }

    public static HologramAPI getHologramAPI() {
        return hologramAPI;
    }

    public static LevelAPI getLevelAPI() {
        return levelAPI;
    }

    public static GroupAPI getGroupAPI() {
        return groupAPI;
    }

    public static ChestShopAPI getChestShopAPI() {
        return chestShopAPI;
    }

    public static QuestAPI getQuestAPI() {
        return questAPI;
    }

    public static SettingsAPI getSettingsAPI() {
        return settingsAPI;
    }

    public static CrateAPI getCrateAPI() {
        return crateAPI;
    }
}
