package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.data.teleportation.Warp;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FwCommand extends PluginCommand<ServerCore> {

    public FwCommand(final ServerCore owner) {
        super("fw", owner);
        this.setDescription("Teleportiere dich schnell zur Farmwelt");
        this.setAliases(Arrays.asList("farmwelt", "farmen").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_NULL:
                        player.sendMessage(Language.get("warp.no.warps"));
                        break;
                    case CALLBACK_ALL_WARPS:
                        final List<String> list = Arrays.asList(delivery.getData());

                        final HashMap<String, Warp> warps = new HashMap<>();

                        list.forEach(z -> {
                            if (!z.equals(delivery.getKey().toLowerCase())) {
                                final String[] d = z.split(">>");
                                warps.put(d[0], new Warp(d[0], d[1], d[2], d[3], d[4], Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9])));
                            }
                        });

                        final Warp warp = warps.get("FarmWelt");
                        if (warp.getServer().equals(this.getPlugin().getServerName())) {
                            player.teleport(new Location(warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch(), this.getPlugin().getServer().getLevelByName(warp.getWorld())));
                            player.sendMessage(Language.get("warp.teleported", warp.getName()));
                        } else {
                            this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "WARP_NULL==&" + "FarmWelt", player.getName(), warp.getServer(), warp.getWorld(),
                                    String.valueOf(warp.getX()), String.valueOf(warp.getY()), String.valueOf(warp.getZ()), String.valueOf(warp.getYaw()), String.valueOf(warp.getPitch()));
                        }

                        break;
                }
            }), Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_WARPS.name(), "null");
        }
        return true;
    }
}
