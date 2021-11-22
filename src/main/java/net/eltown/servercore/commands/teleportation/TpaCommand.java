package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;

public class TpaCommand extends PluginCommand<ServerCore> {

    public TpaCommand(ServerCore owner) {
        super("tpa", owner);
        this.setDescription("Versende eine Teleportationsanfrage an einen anderen Spieler");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length == 0) {
                this.openMain(player);
            } else if (args.length == 1) {
                final String target = args[0];
                if (target.equals(player.getName())) {
                    player.sendMessage(Language.get("tpa.other.player"));
                    return true;
                }
                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                    switch (CoreCalls.valueOf(delivery1.getKey().toUpperCase())) {
                        case CALLBACK_GET_ONLINE_PLAYERS:
                            final List<String> players = Arrays.asList(delivery1.getData()[1].split("#"));
                            if (players.contains(target)) {
                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery2 -> {
                                    switch (TeleportationCalls.valueOf(delivery2.getKey().toUpperCase())) {
                                        case CALLBACK_TPA_ALREADY_SENT:
                                            player.sendMessage(Language.get("tpa.already.sent", target));
                                            break;
                                        case CALLBACK_NULL:
                                            player.sendMessage(Language.get("tpa.sent", target));
                                            break;
                                    }
                                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_SEND_TPA.name(), player.getName(), target);
                            } else player.sendMessage(Language.get("tpa.target.offline", target));
                            break;
                    }
                }), Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
            } else {
                this.openMain(player);
            }

        }
        return true;
    }

    private void openMain(Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            try {
                switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_GET_ONLINE_PLAYERS:
                        final List<String> list = Arrays.asList(delivery.getData()[1].split("#"));

                        if (list.size() == 0) {
                            player.sendMessage(Language.get("tpa.no.players"));
                            return;
                        }

                        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Teleportationsanfrage", "Suche dir den Spieler aus, zu dem du dich teleportieren lassen möchtest.");
                        list.forEach(e -> {
                            if (e.equals(player.getName())) return;
                            form.addButton(new ElementButton("§8» §fAnfrage an:\n§a" + e), g -> {
                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (CoreCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_GET_ONLINE_PLAYERS:
                                            final List<String> players = Arrays.asList(delivery1.getData()[1].split("#"));
                                            if (players.contains(e)) {
                                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery2 -> {
                                                    switch (TeleportationCalls.valueOf(delivery2.getKey().toUpperCase())) {
                                                        case CALLBACK_TPA_ALREADY_SENT:
                                                            player.sendMessage(Language.get("tpa.already.sent", e));
                                                            break;
                                                        case CALLBACK_NULL:
                                                            player.sendMessage(Language.get("tpa.sent", e));
                                                            break;
                                                    }
                                                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_SEND_TPA.name(), player.getName(), e);
                                            }
                                            break;
                                    }
                                }), Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
                            });
                        });

                        form.build().send(player);
                        break;
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }), Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
    }

}