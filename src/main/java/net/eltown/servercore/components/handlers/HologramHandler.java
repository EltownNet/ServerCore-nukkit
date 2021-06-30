package net.eltown.servercore.components.handlers;

import cn.nukkit.Player;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.utils.Config;
import net.eltown.servercore.ServerCore;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class HologramHandler {

    private ServerCore serverCore;
    public final Config config;

    public HashMap<String, FloatingTextParticle> particles = new HashMap<>();

    public HologramHandler(final ServerCore serverCore) {
        this.serverCore = serverCore;
        this.config = new Config(serverCore.getDataFolder() + "/components/holograms.yml", Config.YAML);

        this.config.getSection("holograms").getAll().getKeys(false).forEach(e -> {
            final StringBuilder builder = new StringBuilder();
            this.config.getStringList("holograms." + e + ".lines").forEach(o -> {
                final String[] d = o.split(">:<");
                builder.append(d[1]).append("\n");
            });
            final String text = builder.substring(0, builder.length() - 1);

            this.particles.put(e, new FloatingTextParticle(
                    new Location(this.config.getDouble("holograms." + e + ".x"), this.config.getDouble("holograms." + e + ".y"), this.config.getDouble("holograms." + e + ".z")),
                    this.config.getString("holograms." + e + ".title"),
                    text
            ));
        });
    }

    public void createHologram(final Player player, final String name, final String title, final LinkedList<String> lines) {
        final StringBuilder builder = new StringBuilder();
        lines.forEach(e -> {
            final String[] d = e.split(">:<");
            builder.append(d[1]).append("\n");
        });
        final String text = builder.substring(0, builder.length() - 1);

        final FloatingTextParticle particle = new FloatingTextParticle(player.getLocation(), title, text);
        player.getLevel().addParticle(particle);
        this.particles.put(name, particle);

        this.config.set("holograms." + name + ".title", title);
        this.config.set("holograms." + name + ".lines", lines);
        this.config.set("holograms." + name + ".x", player.getX());
        this.config.set("holograms." + name + ".y", player.getY());
        this.config.set("holograms." + name + ".z", player.getZ());
        this.config.save();
        this.config.reload();
    }

    public boolean hologramExists(final String name) {
        return this.config.exists("holograms." + name);
    }

    public void addHologramLine(final String name, final String line) {
        final List<String> lines = this.config.getStringList("holograms." + name + ".lines");
        lines.add((lines.size() + 1) + ">:<" + line);

        final StringBuilder builder = new StringBuilder();
        lines.forEach(e -> {
            final String[] d = e.split(">:<");
            builder.append(d[1]).append("\n");
        });
        final String text = builder.substring(0, builder.length() - 1);
        this.particles.get(name).setText(text);

        this.config.set("holograms." + name + ".lines", lines);
        this.config.save();
        this.config.reload();
    }

    public void removeLastLine(final String name) {
        final List<String> lines = this.config.getStringList("holograms." + name + ".lines");
        lines.removeIf(s -> s.startsWith(String.valueOf(lines.size())));

        final StringBuilder builder = new StringBuilder();
        lines.forEach(e -> {
            final String[] d = e.split(">:<");
            builder.append(d[1]).append("\n");
        });
        final String text = builder.substring(0, builder.length() - 1);
        this.particles.get(name).setText(text);

        this.config.set("holograms." + name + ".lines", lines);
        this.config.save();
        this.config.reload();
    }

    public void moveHologram(final String name, final Location location) {
        this.particles.get(name).x = location.x;
        this.particles.get(name).y = location.y;
        this.particles.get(name).z = location.z;

        this.config.set("holograms." + name + ".x", location.getX());
        this.config.set("holograms." + name + ".y", location.getY());
        this.config.set("holograms." + name + ".z", location.getZ());
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
    }

    public void deleteHologram(final String name) {
        this.particles.get(name).setInvisible(true);
        this.particles.remove(name);

        Map<String, Object> map = this.config.getSection("holograms").getAllMap();
        map.remove(name);
        this.config.set("holograms", map);
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
    }

    public void updateAllHolograms() {
        this.serverCore.getServer().getOnlinePlayers().values().forEach(e -> {
            this.particles.values().forEach(g -> {
                e.getLevel().addParticle(g);
            });
        });
    }

}
