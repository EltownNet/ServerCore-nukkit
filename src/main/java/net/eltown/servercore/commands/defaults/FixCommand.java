package net.eltown.servercore.commands.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

public class FixCommand extends PluginCommand<ServerCore> {

    public FixCommand(final ServerCore plugin) {
        super("fix", plugin);
        this.setDescription("Behebe lästige Chunk-Fehler.");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender.isPlayer()) {
            final Player player = (Player) sender;
            player.getLevel().getChunks().values().forEach(ch -> {
               if (ch.isGenerated() && ch.isLoaded()) player.unloadChunk(ch.getX(), ch.getZ());
            });
            player.sendMessage("§8» §fCore §8| §7Deine Chunks wurden neugeladen.");
        }

        return true;
    }
}
