package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class ClearCommand extends PluginCommand<ServerCore> {

    public ClearCommand(final ServerCore owner) {
        super("clear", owner);
        this.setDescription("Clear Command");
        this.setPermission("core.command.clear");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 0 && sender instanceof Player) {
            final Player player = (Player) sender;
            player.getInventory().clearAll();
            player.sendMessage(Language.get("clear.cleared"));
        } else if (args.length == 1) {
            final Player player = this.getPlugin().getServer().getPlayer(args[0]);
            if (player != null) {
                player.getInventory().clearAll();
                sender.sendMessage(Language.get("clear.cleared.other", player.getName()));
            } else sender.sendMessage(Language.get("clear.player.not.found"));
        } else sender.sendMessage(Language.get("clear.usage"));
        return true;
    }
}
