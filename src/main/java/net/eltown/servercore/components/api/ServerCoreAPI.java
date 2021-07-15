package net.eltown.servercore.components.api;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.HologramAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;
import net.eltown.servercore.components.api.intern.NpcAPI;

public class ServerCoreAPI {

    private static HologramAPI hologramAPI;
    private static LevelAPI levelAPI;
    private static NpcAPI npcAPI;

    public ServerCoreAPI(final ServerCore serverCore) {
        hologramAPI = serverCore.getHologramAPI();
        levelAPI = serverCore.getLevelAPI();
        npcAPI = serverCore.getNpcAPI();
    }

    public static HologramAPI getHologramAPI() {
        return hologramAPI;
    }

    public static LevelAPI getLevelAPI() {
        return levelAPI;
    }

    public static NpcAPI getNpcAPI() {
        return npcAPI;
    }
}
