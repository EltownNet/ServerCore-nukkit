package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TpacceptCommand extends PluginCommand<ServerCore> {

    public TpacceptCommand(ServerCore owner) {
        super("tpaccept", owner);
        this.setDescription("Nehme eine Teleportationsanfrage eines anderen Spielers an");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openMain(player);
        }
        return true;
    }

    private void openMain(Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    player.sendMessage(Language.get("tpa.no.requests"));
                    break;
                case CALLBACK_TPAS:
                    final List<String> list = Arrays.asList(delivery.getData());
                    final List<String> tpas = new ArrayList<>();

                    list.forEach(z -> {
                        if (!z.equals(delivery.getKey().toLowerCase())) {
                            tpas.add(z);
                        }
                    });

                    final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Teleportationsanfragen", "Wähle eine Anfrage aus, welche du annehmen oder ablehnen möchtest.");
                    tpas.forEach(e -> {
                        form.addButton(new ElementButton("§8» §fAnfrage von\n§a" + e), g -> {
                            final ModalForm modalForm = new ModalForm.Builder("§7» §8Teleportationsanfragen", "§fMöchtest du die Teleportationsanfrage von §a" + e + " §fannehmen oder ablehnen?",
                                    "§8» §aAnnehmen", "§8» §cAblehnen")
                                    .onYes(h -> {
                                        this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_ACCEPT_TPA.name(), player.getName(), e);

                                        final Location location = player.getLocation();
                                        if (this.getPlugin().getServer().getPlayer(e) != null) {
                                            final Player player1 = this.getPlugin().getServer().getPlayer(e);
                                            player1.teleport(location);
                                        } else {
                                            this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "TPA_NULL==&" + player.getName(), e, this.getPlugin().getServerName(),
                                                    location.getLevel().getName(), String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()),
                                                    String.valueOf(location.getPitch()));
                                        }
                                        player.sendMessage(Language.get("tpa.accepted", e));
                                    })
                                    .onNo(h -> {
                                        this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DENY_TPA.name(), player.getName(), e);
                                        this.openMain(h);
                                        player.sendMessage(Language.get("tpa.denied", e));
                                    })
                                    .build();

                            modalForm.send(g);
                        });
                    });

                    form.build().send(player);
                    break;
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_TPAS.name(), player.getName());
    }

}