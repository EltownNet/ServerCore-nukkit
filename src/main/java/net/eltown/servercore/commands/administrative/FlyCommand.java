package net.eltown.servercore.commands.administrative;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class FlyCommand extends PluginCommand<ServerCore> {

    public FlyCommand(final ServerCore owner) {
        super("fly", owner);
        this.setDescription("Fly Command");
        this.setPermission("core.command.fly");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 0 && sender instanceof Player) {
            final Player player = (Player) sender;
            if (player.getAdventureSettings().get(AdventureSettings.Type.ALLOW_FLIGHT)) {
                player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, false);
                player.sendMessage(Language.get("fly.disabled"));
            } else {
                player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
                player.sendMessage(Language.get("fly.enabled"));
            }
            player.getAdventureSettings().update();
        } else if (args.length == 1) {
            final Player player = this.getPlugin().getServer().getPlayer(args[0]);
            if (player != null) {
                if (player.getAdventureSettings().get(AdventureSettings.Type.ALLOW_FLIGHT)) {
                    player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, false);
                    sender.sendMessage(Language.get("fly.disabled.other", player.getName()));
                } else {
                    player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
                    sender.sendMessage(Language.get("fly.enabled.other", player.getName()));
                }
                player.getAdventureSettings().update();
            } else sender.sendMessage(Language.get("fly.player.not.found"));
        } else sender.sendMessage(Language.get("fly.usage"));
        return true;
    }
}
