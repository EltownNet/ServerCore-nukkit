package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.item.Item;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class RepairCommand extends PluginCommand<ServerCore> {

    public RepairCommand(final ServerCore owner) {
        super("repair", owner);
        this.setDescription("Repair Command");
        this.setPermission("core.command.repair");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final Item item = player.getInventory().getItemInHand();
            if (item.isTool() || item.isArmor() || item.isShears()) {
                item.setDamage(0);
                player.getInventory().setItemInHand(item);
                player.sendMessage(Language.get("repair.repaired"));
            } else player.sendMessage(Language.get("repair.invalid.item"));
        }
        return true;
    }
}
