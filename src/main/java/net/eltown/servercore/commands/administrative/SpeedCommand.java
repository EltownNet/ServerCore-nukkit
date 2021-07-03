package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

public class SpeedCommand extends PluginCommand<ServerCore> {

    public SpeedCommand(final ServerCore plugin) {
        super("speed", plugin);
        this.setDescription("Verändere deine Geschwindigkeit.");
        this.setPermission("core.command.speed");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (args.length <= 1) {
            if (sender.isPlayer() && sender.hasPermission(this.getPermission())) {

                final Player player = (Player) sender;

                if (args.length == 0) {
                    sender.sendMessage(Language.get("speed.usage"));
                    return true;
                }
                try {
                    final float speed = Float.parseFloat(args[0]);

                    if (speed > 0) {
                        player.setMovementSpeed(speed / 10);
                        player.sendMessage(Language.get("speed.adjusted", speed));
                    } else sender.sendMessage(Language.get("speed.invalid.number"));

                } catch (Exception ex) {
                    sender.sendMessage(Language.get("speed.invalid.number"));
                }
            }
        } else {
            if (sender.hasPermission("core.command.speed.other")) {
                try {
                    final float speed = Float.parseFloat(args[0]);

                    if (speed > 0) {

                        final Player player = Server.getInstance().getPlayer(args[1]);

                        if (player != null) {
                            player.setMovementSpeed(speed / 10);
                            player.sendMessage(Language.get("speed.adjusted", speed));
                            sender.sendMessage(Language.get("speed.set", player.getName(), speed));
                        } else sender.sendMessage(Language.get("speed.pnf"));

                    } else sender.sendMessage(Language.get("speed.invalid.number"));

                } catch (Exception ex) {
                    sender.sendMessage(Language.get("speed.invalid.number"));
                }
            }
        }
        return false;
    }
}
