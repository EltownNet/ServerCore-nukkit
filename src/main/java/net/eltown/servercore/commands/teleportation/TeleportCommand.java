package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Collections;

public class TeleportCommand extends PluginCommand<ServerCore> {

    public TeleportCommand(ServerCore owner) {
        super("teleport", owner);
        this.setDescription("Teleport command");
        this.setPermission("core.command.teleport");
        this.setAliases(Collections.singletonList("tp").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length == 1) {
                final Player target = this.getPlugin().getServer().getPlayer(args[0]);
                if (target != null) {
                    player.teleport(target.getLocation());
                    player.sendMessage(Language.get("teleport.teleported.target", target.getName()));
                } else {
                    this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                        switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_NULL:
                                player.sendMessage(Language.get("teleport.player.not.online", args[0]));
                                break;
                            case CALLBACK_ONLINE:
                                this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "TP_NULL==&" + args[0], player.getName(), "null", args[0],
                                        "0", "0", "0", "0", "0");
                                player.sendMessage(Language.get("teleport.teleported.target", args[0]));
                                break;
                        }
                    }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), args[0]);
                }
            } else if (args.length == 2) {
                final Player from = this.getPlugin().getServer().getPlayer(args[0]);
                final Player to = this.getPlugin().getServer().getPlayer(args[1]);
                if (from != null && to != null) {
                    from.teleport(to.getLocation());
                    player.sendMessage(Language.get("teleport.teleported.others", args[0], args[1]));
                } else {
                    this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                        switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_NULL:
                                player.sendMessage(Language.get("teleport.player.not.online", args[0]));
                                break;
                            case CALLBACK_ONLINE:
                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery1 -> {
                                    switch (CoreCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_NULL:
                                            player.sendMessage(Language.get("teleport.player.not.online", args[1]));
                                            break;
                                        case CALLBACK_ONLINE:
                                            this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "TP_NULL==&" + args[1], args[0], "null", args[1],
                                                    "0", "0", "0", "0", "0");
                                            player.sendMessage(Language.get("teleport.teleported.others", args[0], args[1]));
                                            break;
                                    }
                                }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), args[1]);
                                break;
                        }
                    }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), args[0]);
                }
            } else if (args.length == 3) {
                try {
                    final int x = Integer.parseInt(args[0]);
                    final int y = Integer.parseInt(args[1]);
                    final int z = Integer.parseInt(args[2]);
                    player.teleport(new Location(x, y, z, player.getLevel()));
                    player.sendMessage(Language.get("teleport.teleported.xyz", x, y, z));
                } catch (final Exception ignored) {
                    player.sendMessage(Language.get("teleport.invalid.coordiantes"));
                }
            } else if (args.length == 4) {
                try {
                    final Player target = this.getPlugin().getServer().getPlayer(args[0]);
                    if (target == null) {
                        player.sendMessage(Language.get("teleport.player.not.online", args[0]));
                        return true;
                    }
                    final int x = Integer.parseInt(args[1]);
                    final int y = Integer.parseInt(args[2]);
                    final int z = Integer.parseInt(args[3]);
                    target.teleport(new Location(x, y, z, target.getLevel()));
                    player.sendMessage(Language.get("teleport.teleported.xyz.other", target.getName(), x, y, z));
                } catch (final Exception ignored) {
                    player.sendMessage(Language.get("teleport.invalid.coordiantes"));
                }
            } else player.sendMessage(Language.get("teleport.usage", this.getName()));
        }
        return true;
    }

}