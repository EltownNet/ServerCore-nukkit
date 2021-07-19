package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class HealCommand extends PluginCommand<ServerCore> {

    public HealCommand(final ServerCore owner) {
        super("heal", owner);
        this.setDescription("Heal Command");
        this.setPermission("core.command.heal");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 0 && sender instanceof Player) {
            final Player player = (Player) sender;
            player.setHealth(20);
            player.getFoodData().setLevel(20, 20);
            player.sendMessage(Language.get("heal.healed"));
        } else if (args.length == 1) {
            final Player player = this.getPlugin().getServer().getPlayer(args[0]);
            if (player != null) {
                player.setHealth(20);
                player.getFoodData().setLevel(20, 20);
                sender.sendMessage(Language.get("heal.healed.other", player.getName()));
            } else sender.sendMessage(Language.get("heal.player.not.online"));
        } else sender.sendMessage(Language.get("heal.usage"));
        return true;
    }
}
