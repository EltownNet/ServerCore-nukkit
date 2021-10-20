package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.level.LevelReward;
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
                    final LevelReward levelReward = new LevelReward(Integer.parseInt(delivery.getData()[1]), delivery.getData()[2], delivery.getData()[3]);
                    final String[] rewardData = levelReward.getData().split("#");

                    player.sendMessage(Language.get("level.reward", levelReward.getDescription()));

                    if (rewardData[0].startsWith("gutschein")) {
                        this.instance.getTinyRabbit().sendAndReceive(delivery1 -> {
                            switch (GiftkeyCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                case CALLBACK_NULL:
                                    player.sendMessage(Language.get("level.reward.giftkey", delivery1.getData()[1]));
                                    break;
                            }
                        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(1), rewardData[1], player.getName());
                    } else if (rewardData[0].startsWith("item")) {
                        final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(rewardData[1]);
                        player.getInventory().addItem(item);
                    } else if (rewardData[0].startsWith("permission")) {
                        this.instance.getGroupAPI().addPlayerPermission(player.getName(), rewardData[1]);
                    }
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
