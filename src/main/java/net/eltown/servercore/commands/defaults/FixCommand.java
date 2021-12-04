package net.eltown.servercore.commands.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Sound;
import net.eltown.servercore.ServerCore;

public class FixCommand extends PluginCommand<ServerCore> {

    public FixCommand(final ServerCore plugin) {
        super("fix", plugin);
        this.setDescription("Behebe lästige Anzeigefehler.");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender.isPlayer()) {
            final Player player = (Player) sender;
            player.getLevel().getChunks().values().forEach(ch -> {
               if (ch.isGenerated() && ch.isLoaded()) player.unloadChunk(ch.getX(), ch.getZ());
            });
            this.getPlugin().getHologramAPI().updateAllHolograms(player);
            this.getPlugin().getHologramAPI().updateSpecialHolograms(player);
            if (this.getPlugin().getServerName().equals("server-1")) player.getLevel().addParticle(this.getPlugin().getFeatureRoleplay().crateHologram, player);

            this.getPlugin().playSound(player, Sound.MOB_VILLAGER_YES);
            player.sendMessage("§8» §fCore §8| §7Mögliche Anzeigefehler sollten nun behoben sein.");
        }

        return true;
    }
}
