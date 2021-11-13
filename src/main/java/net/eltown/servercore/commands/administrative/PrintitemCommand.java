package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;

public class PrintitemCommand extends PluginCommand<ServerCore> {

    public PrintitemCommand(final ServerCore owner) {
        super("printitem", owner);
        this.setDescription("Lasse das Item in deiner Hand ausdrucken.");
        this.setPermission("core.command.printitem");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.getPlugin().getLogger().info("[PRINT-ITEM] " + SyncAPI.ItemAPI.pureItemToStringWithCount(player.getInventory().getItemInHand()));
            player.sendMessage("Item ausgedruckt. Du kannst die Daten in der Konsole einsehen.");
        }
        return true;
    }
}
