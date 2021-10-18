package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.scoreboard.network.DisplayEntry;
import net.eltown.servercore.components.scoreboard.network.ScoreboardDisplay;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.HashMap;

@RequiredArgsConstructor
public class LevelAPI {

    private final ServerCore instance;

    public final HashMap<String, Level> cachedData = new HashMap<>();

    public void addExperience(final Player player, final double experience) {
        final Level level = this.cachedData.get(player.getName());
        level.setExperience(level.getExperience() + experience);

        final ScoreboardDisplay scoreboardDisplay = ScoreboardAPI.cachedData.get(player.getName());
        final DisplayEntry displayEntry = ScoreboardAPI.cachedDisplayEntries.get(player.getName() + "/level");
        scoreboardDisplay.removeEntry(displayEntry);

        final DisplayEntry levelEntry = scoreboardDisplay.addLine("   §f" + this.instance.getLevelAPI().getLevel(player.getName()).getLevel() + " §8[" + this.instance.getLevelAPI().getLevelDisplay(player) + "§8]  ", 5);
        ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/level", levelEntry);

        player.sendActionBar("§a+ §2" + experience + "XP");

        this.checkForLevelUp(player);
    }

    public void checkForLevelUp(final Player player) {
        final Level level = this.cachedData.get(player.getName());
        final double experience = this.getMaxExperienceByLevel(level.getLevel());

        if (level.getExperience() >= experience) this.levelUp(player);
    }

    public void levelUp(final Player player) {
        final Level level = this.cachedData.get(player.getName());
        level.setLevel(level.getLevel() + 1);

        player.sendMessage(" ");
        player.sendMessage(Language.get("level.levelup", level.getLevel()));
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_LEVEL_REWARD:
                    final String levelReward = delivery.getData()[1];
                    player.sendMessage(Language.get("level.reward", levelReward));
                    break;
            }
        }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_LEVEL_REWARD.name(), String.valueOf(level.getLevel()));
        player.sendMessage(" ");

        player.setScoreTag("§gLevel §l" + level.getLevel());

        final ScoreboardDisplay scoreboardDisplay = ScoreboardAPI.cachedData.get(player.getName());
        final DisplayEntry displayEntry = ScoreboardAPI.cachedDisplayEntries.get(player.getName() + "/level");
        scoreboardDisplay.removeEntry(displayEntry);

        final DisplayEntry levelEntry = scoreboardDisplay.addLine("   §f" + this.instance.getLevelAPI().getLevel(player.getName()).getLevel() + " §8[" + this.instance.getLevelAPI().getLevelDisplay(player) + "§8]  ", 5);
        ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/level", levelEntry);
    }

    public Level getLevel(final String player) {
        return this.cachedData.get(player);
    }

    public double getMaxExperienceByLevel(final int level) {
        return (level * 500 * (level * 0.66 + (level * 0.25 + 1)));
    }

    public String getLevelDisplay(final Player player) {
        final int level = this.cachedData.get(player.getName()).getLevel();
        final double xp = this.cachedData.get(player.getName()).getExperience() - this.getMaxExperienceByLevel(level - 1); // 5.0 = Derzeitige XP getten
        final double required = this.getMaxExperienceByLevel(level) - this.getMaxExperienceByLevel(level - 1);

        final double percent = (xp / required) * 100;

        final long green = Math.round(percent / 5);

        final StringBuilder builder = new StringBuilder();

        for (int i = 1; i <= 20; i++) {
            builder.append(i <= green ? "§2|" : "§7|");
        }

        return builder.toString();
    }

}
