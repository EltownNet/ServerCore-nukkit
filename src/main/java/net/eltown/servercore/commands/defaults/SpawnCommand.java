package net.eltown.servercore.commands.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class SpawnCommand extends PluginCommand<ServerCore> {

    public static Location spawnLocation;

    public SpawnCommand(final ServerCore owner) {
        super("spawn", owner);
        this.setDescription("Teleportiere dich zum Spawnpunkt des aktuellen Servers");

        spawnLocation = new Location(
                owner.getConfig().getDouble("spawn.x"),
                owner.getConfig().getDouble("spawn.y"),
                owner.getConfig().getDouble("spawn.z"),
                owner.getConfig().getDouble("spawn.yaw"),
                owner.getConfig().getDouble("spawn.pitch"),
                owner.getServer().getLevelByName(owner.getConfig().getString("spawn.level"))
        );
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            player.teleport(spawnLocation);
            player.sendMessage(Language.get("spawn.teleported"));
        }
        return true;
    }
}
