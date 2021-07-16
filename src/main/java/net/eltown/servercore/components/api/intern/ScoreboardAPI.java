package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import net.eltown.servercore.components.scoreboard.network.DisplayEntry;
import net.eltown.servercore.components.scoreboard.network.Scoreboard;
import net.eltown.servercore.components.scoreboard.network.ScoreboardDisplay;

import java.util.HashMap;

public class ScoreboardAPI {

    public static HashMap<String, ScoreboardDisplay> cachedData = new HashMap<>();
    public static HashMap<String, DisplayEntry> cachedDisplayEntries = new HashMap<>();

    public static Scoreboard createScoreboard() {
        return new Scoreboard();
    }

    public static void setScoreboard(Player player, Scoreboard scoreboard) {
        scoreboard.showFor(player);
    }

    public static void removeScorebaord(Player player, Scoreboard scoreboard) {
        scoreboard.hideFor(player);
    }

}
