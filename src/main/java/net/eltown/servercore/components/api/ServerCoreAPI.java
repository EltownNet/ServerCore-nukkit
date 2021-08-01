package net.eltown.servercore.components.api;

import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.HologramAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;

public class ServerCoreAPI {

    private static HologramAPI hologramAPI;
    private static LevelAPI levelAPI;

    public ServerCoreAPI(final ServerCore serverCore) {
        hologramAPI = serverCore.getHologramAPI();
        levelAPI = serverCore.getLevelAPI();
    }

    public static HologramAPI getHologramAPI() {
        return hologramAPI;
    }

    public static LevelAPI getLevelAPI() {
        return levelAPI;
    }

}
