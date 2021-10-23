package net.eltown.servercore.commands.administrative;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

public class SetSpawnProtectionCommand extends PluginCommand<ServerCore> {

    public static int spawnProtection;

    public SetSpawnProtectionCommand(final ServerCore plugin) {
        super("setspawnprotection", plugin);
        this.setPermission("core.setspawnprotection");

        spawnProtection = this.getPlugin().getConfig().exists("spawnProtection") ? this.getPlugin().getConfig().getInt("spawnProtection") : 0;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length > 0) {
            try {
                final int radius = Integer.parseInt(args[0]);

                this.getPlugin().getConfig().set("spawnProtection", radius);
                this.getPlugin().getConfig().save();
                this.getPlugin().getConfig().reload();

                spawnProtection = radius;
            } catch (Exception ex) {
                sender.sendMessage("§cBitte gebe einen gültigen Radius an.");
            }
        } else sender.sendMessage("§c/setspawnprotection <radius>");
        return true;
    }
}
