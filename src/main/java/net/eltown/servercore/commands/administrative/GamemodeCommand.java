package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

import java.util.Arrays;

public class GamemodeCommand extends PluginCommand<ServerCore> {

    public GamemodeCommand(final ServerCore owner) {
        super("gamemode", owner);
        this.setDescription("Gamemode Command");
        this.setPermission("core.command.gamemode");
        this.setAliases(Arrays.asList("gm", "mode").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (args.length == 1 && sender instanceof Player) {
            final Player player = (Player) sender;
            switch (args[0]) {
                case "survival":
                case "0":
                    player.setGamemode(0);
                    player.sendMessage(Language.get("gamemode.set", 0));
                    break;
                case "creative":
                case "1":
                    player.setGamemode(1);
                    player.sendMessage(Language.get("gamemode.set", 1));
                    break;
                case "adventure":
                case "2":
                    player.setGamemode(2);
                    player.sendMessage(Language.get("gamemode.set", 2));
                    break;
                case "spectator":
                case "3":
                    player.setGamemode(3);
                    player.sendMessage(Language.get("gamemode.set", 3));
                    break;
                default:
                    player.sendMessage(Language.get("gamemode.mode.invalid"));
                    break;
            }
        } else if (args.length == 2) {
            final Player player = this.getPlugin().getServer().getPlayer(args[1]);
            if (player != null) {
                switch (args[0]) {
                    case "survival":
                    case "0":
                        player.setGamemode(0);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 0));
                        break;
                    case "creative":
                    case "1":
                        player.setGamemode(1);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 1));
                        break;
                    case "adventure":
                    case "2":
                        player.setGamemode(2);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 2));
                        break;
                    case "spectator":
                    case "3":
                        player.setGamemode(3);
                        sender.sendMessage(Language.get("gamemode.set.other", player.getName(), 3));
                        break;
                    default:
                        sender.sendMessage(Language.get("gamemode.mode.invalid"));
                        break;
                }
            } else sender.sendMessage(Language.get("gamemode.player.not.found"));
        } else sender.sendMessage(Language.get("gamemode.usage"));
        return true;
    }
}
