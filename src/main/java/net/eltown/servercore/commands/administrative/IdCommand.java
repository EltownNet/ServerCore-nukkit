package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class IdCommand extends PluginCommand<ServerCore> {

    public IdCommand(final ServerCore owner) {
        super("id", owner);
        this.setDescription("Erhalte die ID des Items in deiner Hand");
        this.setPermission("core.command.id");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            player.sendMessage(Language.get("id.info", player.getInventory().getItemInHand().getId(), player.getInventory().getItemInHand().getDamage()));
        }
        return true;
    }
}
