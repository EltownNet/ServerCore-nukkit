package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class EnchantCommand extends PluginCommand<ServerCore> {

    public EnchantCommand(final ServerCore owner) {
        super("enchant", owner);
        this.setDescription("Enchant Command");
        this.setPermission("core.command.enchant");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length == 3) {
                final Player target = this.getPlugin().getServer().getPlayer(args[0]);
                if (target != null) {
                    try {
                        final int enchantmentId = Integer.parseInt(args[1]);
                        final int enchantLevel = Integer.parseInt(args[2]);
                        if (enchantmentId >= 0 && enchantmentId <= 36) {
                            this.getPlugin().getCustomEnchantment().enchantItem(target, enchantmentId, enchantLevel);
                        } else {
                            this.getPlugin().getCustomEnchantment().enchantItem(target, this.getPlugin().getCustomEnchantment().enchantmentId.get(enchantmentId), enchantLevel);
                        }
                        player.sendMessage(Language.get("enchant.enchanted"));
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("enchant.invalid.arguments"));
                    }
                } else player.sendMessage(Language.get("enchant.player.not.found"));
            } else player.sendMessage(Language.get("enchant.usage", this.getName()));
        }
        return true;
    }
}
