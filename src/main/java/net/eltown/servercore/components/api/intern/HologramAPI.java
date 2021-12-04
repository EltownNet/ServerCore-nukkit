package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.level.Location;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.utils.Config;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.quests.FullQuestPlayer;

import java.util.*;

public class HologramAPI {

    private final ServerCore serverCore;
    public final Config config;

    public HashMap<String, FloatingTextParticle> particles = new HashMap<>();
    public List<String> noUpdate = new ArrayList<>();

    public HologramAPI(final ServerCore serverCore) {
        this.serverCore = serverCore;
        this.config = new Config(serverCore.getDataFolder() + "/components/holograms.yml", Config.YAML);

        this.config.getSection("holograms").getAll().getKeys(false).forEach(e -> {
            final StringBuilder builder = new StringBuilder();
            this.config.getStringList("holograms." + e + ".lines").forEach(o -> builder.append(o).append("\n"));
            String text;
            if (this.config.getStringList("holograms." + e + ".lines").size() == 0) text = "";
            else text = builder.substring(0, builder.length() - 1);

            final FloatingTextParticle floatingTextParticle;
            if (text.isEmpty()) floatingTextParticle = new FloatingTextParticle(new Location(this.config.getDouble("holograms." + e + ".x"), this.config.getDouble("holograms." + e + ".y"), this.config.getDouble("holograms." + e + ".z")), this.config.getString("holograms." + e + ".title"));
            else floatingTextParticle = new FloatingTextParticle(new Location(this.config.getDouble("holograms." + e + ".x"), this.config.getDouble("holograms." + e + ".y"), this.config.getDouble("holograms." + e + ".z")), this.config.getString("holograms." + e + ".title"), text);
            this.particles.put(e, floatingTextParticle);
        });

        this.noUpdate.add("ainara");
        this.noUpdate.add("mike");
    }

    public void createHologram(final Player player, final String name, final String title, final LinkedList<String> lines) {
        final StringBuilder builder = new StringBuilder();
        lines.forEach(e -> builder.append(e).append("\n"));
        String text;
        if (lines.size() == 0) text = "";
        else text = builder.substring(0, builder.length() - 1);

        FloatingTextParticle particle;
        if (text.isEmpty()) particle = new FloatingTextParticle(player.getLocation(), title);
        else particle = new FloatingTextParticle(player.getLocation(), title, text);

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
        lines.add(line);

        final StringBuilder builder = new StringBuilder();
        lines.forEach(e -> builder.append(e).append("\n"));
        final String text = builder.substring(0, builder.length() - 1);
        this.particles.get(name).setText(text);

        this.config.set("holograms." + name + ".lines", lines);
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
    }

    public void removeLine(final String name, final int i) {
        final List<String> lines = this.config.getStringList("holograms." + name + ".lines");
        lines.removeIf(s -> s.startsWith(lines.get(i - 1)));

        final StringBuilder builder = new StringBuilder();
        if (lines.size() != 0) {
            lines.forEach(e -> builder.append(e).append("\n"));
            final String text = builder.substring(0, builder.length() - 1);
            this.particles.get(name).setText(text);
        } else this.particles.get(name).setText("");

        this.config.set("holograms." + name + ".lines", lines);
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
    }

    public void setTitle(final String name, final String title) {
        this.particles.get(name).setTitle(title);

        this.config.set("holograms." + name + ".title", title);
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
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
        final RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.eid = this.particles.get(name).getEntityId();
        Server.broadcastPacket(Server.getInstance().getOnlinePlayers().values(), packet);
        this.particles.remove(name);

        Map<String, Object> map = this.config.getSection("holograms").getAllMap();
        map.remove(name);
        this.config.set("holograms", map);
        this.config.save();
        this.config.reload();

        this.updateAllHolograms();
    }

    public void hideHologram(final String name, final Player player) {
        final RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.eid = this.particles.get(name).getEntityId();
        player.dataPacket(packet);
    }

    public int getHologramLines(final String name) {
        return this.config.getStringList("holograms." + name + ".lines").size();
    }

    public void updateSpecialHolograms(final Player player) {
        if (!this.serverCore.getServerName().equals("server-1")) return;

        final Set<FullQuestPlayer> quests = this.serverCore.getQuestAPI().getActivePlayerQuests(player.getName());
        this.serverCore.getQuestAPI().checkIfQuestIsExpired(player.getName());

        //Ainara
        int ainara = 0;
        for (final FullQuestPlayer quest : quests) {
            if (quest.getQuest().getLink().equals("Ainara")) ainara++;
        }
        if (ainara == 0) {
            this.serverCore.getServer().getDefaultLevel().addParticle(this.serverCore.getHologramAPI().particles.get("ainara"), player);
        } else {
            this.serverCore.getHologramAPI().hideHologram("ainara", player);
        }

        //Mike
        int mike = 0;
        for (final FullQuestPlayer quest : quests) {
            if (quest.getQuest().getLink().equals("Mike")) mike++;
        }
        if (mike == 0) {
            this.serverCore.getServer().getDefaultLevel().addParticle(this.serverCore.getHologramAPI().particles.get("mike"), player);
        } else {
            this.serverCore.getHologramAPI().hideHologram("mike", player);
        }
    }

    public void updateAllHolograms() {
        this.serverCore.getServer().getOnlinePlayers().values().forEach(e -> {
            this.particles.forEach((n, p) -> {
                if (!this.noUpdate.contains(n)) {
                    e.getLevel().addParticle(p);
                }
            });
        });
    }

    public void updateAllHolograms(final Player player) {
        this.serverCore.getServer().getOnlinePlayers().values().forEach(e -> {
            this.particles.forEach((n, p) -> {
                if (!this.noUpdate.contains(n)) {
                    e.getLevel().addParticle(p, player);
                }
            });
        });
    }

}
