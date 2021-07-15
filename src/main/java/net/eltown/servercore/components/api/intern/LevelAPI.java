package net.eltown.servercore.components.api.intern;

import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.Level;

import java.util.HashMap;

@RequiredArgsConstructor
public class LevelAPI {

    private final ServerCore instance;

    public final HashMap<String, Level> cachedData = new HashMap<>();

    public void addExperience(final String player, final double experience) {
        final Level level = this.cachedData.get(player);
        level.setExperience(level.getExperience() + experience);
        this.checkForLevelUp(player);
    }

    public void checkForLevelUp(final String player) {
        final Level level = this.cachedData.get(player);
        final double experience = this.getMaxExperienceByLevel(level.getLevel());

        if (level.getExperience() >= experience) this.levelUp(player);
    }

    public void levelUp(final String player) {
        final Level level = this.cachedData.get(player);
        level.setLevel(level.getLevel() + 1);
    }

    public Level getLevel(final String player) {
        return this.cachedData.get(player);
    }

    public double getMaxExperienceByLevel(final int level) {
        return (level * 500 * (level * 0.66 + (level * 0.25 + 1)));
    }

}
