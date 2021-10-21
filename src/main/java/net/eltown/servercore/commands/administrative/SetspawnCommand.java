package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.defaults.SpawnCommand;

public class SetspawnCommand extends PluginCommand<ServerCore> {

    public SetspawnCommand(final ServerCore owner) {
        super("setspawn", owner);
        this.setPermission("core.command.setspawn");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final Location location = player.getLocation();

            this.getPlugin().getConfig().set("spawn.x", location.x);
            this.getPlugin().getConfig().set("spawn.y", location.y);
            this.getPlugin().getConfig().set("spawn.z", location.z);
            this.getPlugin().getConfig().set("spawn.yaw", location.yaw);
            this.getPlugin().getConfig().set("spawn.pitch", location.pitch);
            this.getPlugin().getConfig().set("spawn.level", location.level.getName());
            this.getPlugin().getConfig().save();
            this.getPlugin().getConfig().reload();

            SpawnCommand.spawnLocation = player.getLocation();

            player.sendMessage("Der Spawnpunkt dieses Servers wurde umgesetzt.");
        }
        return true;
    }
}
