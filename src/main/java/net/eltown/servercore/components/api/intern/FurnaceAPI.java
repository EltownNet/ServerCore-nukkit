package net.eltown.servercore.components.api.intern;

import cn.nukkit.level.Location;
import cn.nukkit.utils.Config;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.furnace.Furnace;

import java.util.HashMap;
import java.util.Map;

public class FurnaceAPI {

    private final ServerCore instance;
    private final Config config;

    public HashMap<Integer, Furnace.FurnaceLevel> cachedFurnaceLevel = new HashMap<>();
    public HashMap<Location, Furnace> cachedFurnaces = new HashMap<>();

    public FurnaceAPI(final ServerCore instance) {
        this.instance = instance;
        this.config = new Config(instance.getDataFolder() + "/components/furnace.yml", Config.YAML);

        this.cachedFurnaceLevel.put(0, new Furnace.FurnaceLevel(0,0, 0, 0, 0));
        this.cachedFurnaceLevel.put(1, new Furnace.FurnaceLevel(1,1000, 5, 8, 5));
        this.cachedFurnaceLevel.put(2, new Furnace.FurnaceLevel(2,2000, 8, 16, 10));
        this.cachedFurnaceLevel.put(3, new Furnace.FurnaceLevel(3,4000, 11, 24, 15));
        this.cachedFurnaceLevel.put(4, new Furnace.FurnaceLevel(4,6000, 15, 32, 25));
        this.cachedFurnaceLevel.put(5, new Furnace.FurnaceLevel(5,8000, 20, 48, 30));
        this.cachedFurnaceLevel.put(6, new Furnace.FurnaceLevel(6,10000, 30, 64, 40));
        this.cachedFurnaceLevel.put(7, new Furnace.FurnaceLevel(7,15000, 45, 80, 55));

        for (final String owner : this.config.getSection("furnace").getKeys(false)) {
            for (final String id : this.config.getSection("furnace." + owner).getKeys(false)) {
                final Location location = new Location(
                        this.config.getDouble("furnace." + owner + "." + id + ".x"),
                        this.config.getDouble("furnace." + owner + "." + id + ".y"),
                        this.config.getDouble("furnace." + owner + "." + id + ".z"),
                        this.instance.getServer().getLevelByName(this.config.getString("furnace." + owner + "." + id + ".level"))
                );

                this.cachedFurnaces.put(location,
                        new Furnace(
                                owner,
                                Long.parseLong(id),
                                location,
                                this.cachedFurnaceLevel.get(this.config.getInt("furnace." + owner + "." + id + ".data.level")),
                                this.config.getInt("furnace." + owner + "." + id + ".data.smelting"),
                                this.config.getInt("furnace." + owner + "." + id + ".data.double")
                        )
                );
            }
        }
    }

    public void placeFurnace(final String player, final Location location) {
        final String path = "furnace." + player + "." + this.instance.createNumberId(15) + ".";
        this.config.set(path + "x", location.x);
        this.config.set(path + "y", location.y);
        this.config.set(path + "z", location.z);
        this.config.set(path + "level", location.getLevel().getName());
        this.config.set(path + "data.level", 0);
        this.config.set(path + "data.smelting", 0);
        this.config.set(path + "data.double", 0);
        this.config.save();
        this.config.reload();

        this.cachedFurnaces.put(location, new Furnace(player, Long.parseLong(path.split("\\.")[2]), location, this.cachedFurnaceLevel.get(0), 0, 0));
    }

    public void placeFurnace(final String player, final Location location, final Furnace furnace) {
        final String path = "furnace." + player + "." + this.instance.createNumberId(15) + ".";
        this.config.set(path + "x", location.x);
        this.config.set(path + "y", location.y);
        this.config.set(path + "z", location.z);
        this.config.set(path + "level", location.getLevel().getName());
        this.config.set(path + "data.level", furnace.getFurnaceLevel().getLevel());
        this.config.set(path + "data.smelting", furnace.getSmeltingPercent());
        this.config.set(path + "data.double", furnace.getDoublePercent());
        this.config.save();
        this.config.reload();

        this.cachedFurnaces.put(location, new Furnace(player, Long.parseLong(path.split("\\.")[2]), location, furnace.getFurnaceLevel(), furnace.getSmeltingPercent(), furnace.getDoublePercent()));
    }

    public void upgradeFurnace(final Furnace furnace, final Furnace.FurnaceLevel furnaceLevel) {
        this.config.set("furnace." + furnace.getOwner() + "." + furnace.getId() + ".data.level", furnaceLevel.getLevel());
        this.config.save();
        this.config.reload();

        this.cachedFurnaces.get(furnace.getLocation()).setFurnaceLevel(furnaceLevel);
    }

    public void breakFurnace(final Furnace furnace) {
        final Map<String, Object> map = this.config.getSection("furnace." + furnace.getOwner()).getAllMap();
        map.remove(String.valueOf(furnace.getId()));
        this.config.set("furnace." + furnace.getOwner(), map);
        this.config.save();
        this.config.reload();
    }

}
