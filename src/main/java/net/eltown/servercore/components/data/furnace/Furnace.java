package net.eltown.servercore.components.data.furnace;

import cn.nukkit.level.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Furnace {

    private String owner;
    private final long id;
    private Location location;
    private FurnaceLevel furnaceLevel;
    private int smeltingPercent;
    private int doublePercent;

    @AllArgsConstructor
    @Getter
    public static class FurnaceLevel {

        private final int level;
        private int value;
        private int levelValue;
        private int smeltingPercent;
        private int doublePercent;

    }

}
