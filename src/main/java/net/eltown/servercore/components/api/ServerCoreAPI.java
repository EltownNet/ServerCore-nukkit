package net.eltown.servercore.components.api;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.GroupAPI;
import net.eltown.servercore.components.api.intern.HologramAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;

public class ServerCoreAPI {

    private static HologramAPI hologramAPI;
    private static LevelAPI levelAPI;
    private static GroupAPI groupAPI;

    public ServerCoreAPI(final ServerCore serverCore) {
        hologramAPI = serverCore.getHologramAPI();
        levelAPI = serverCore.getLevelAPI();
        groupAPI = serverCore.getGroupAPI();
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
}
