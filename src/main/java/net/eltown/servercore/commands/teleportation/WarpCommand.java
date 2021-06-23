package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.data.teleportation.Warp;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WarpCommand extends PluginCommand<ServerCore> {

    public WarpCommand(ServerCore owner) {
        super("warp", owner);
        this.setDescription("Teleportiere dich zu verschiedene Orte");
        this.setAliases(Arrays.asList("w", "servers").toArray(new String[]{}));
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
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    player.sendMessage(Language.get("home.no.homes"));
                    break;
                case CALLBACK_ALL_WARPS:
                    final List<String> list = Arrays.asList(delivery.getData());

                    final HashMap<String, Warp> warps = new HashMap<>();
                    final List<String> warpNames = new ArrayList<>();

                    list.forEach(z -> {
                        if (!z.equals(delivery.getKey().toLowerCase())) {
                            final String[] d = z.split(">>");
                            warps.put(d[0], new Warp(d[0], d[1], d[2], d[3], d[4], Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9])));
                            warpNames.add(d[0]);
                        }
                    });

                    final SimpleForm.Builder simpleForm = new SimpleForm.Builder("§7» §8Teleportation", "");
                    warpNames.forEach(e -> {
                        final Warp warp = warps.get(e);
                        simpleForm.addButton(new ElementButton(warp.getDisplayName(), new ElementButtonImageData("url", "http://" + warp.getImageUrl())), h -> {
                            if (warp.getServer().equals(this.getPlugin().getServerName())) {
                                player.teleport(new Location(warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch(), this.getPlugin().getServer().getLevelByName(warp.getWorld())));
                                player.sendMessage(Language.get("warp.teleported", warp.getName()));
                            } else {
                                this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), "WARP_NULL==&" + e, player.getName(), warp.getServer(), warp.getWorld(),
                                        String.valueOf(warp.getX()), String.valueOf(warp.getY()), String.valueOf(warp.getZ()), String.valueOf(warp.getYaw()), String.valueOf(warp.getPitch()));
                            }
                        });
                    });

                    if (player.isOp()) {
                        simpleForm.addButton(new ElementButton("§7» §4Einstellungen"), this::openSettings);
                    }

                    simpleForm.build().send(player);
                    break;
            }
        }), Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_WARPS.name(), "null");
    }

    private void openSettings(final Player player) {
        final SimpleForm simpleForm = new SimpleForm.Builder("§7» §8Einstellungen", "")
                .addButton(new ElementButton("§7» §fWarp hinzufügen"), this::openAddWarp)
                .addButton(new ElementButton("§7» §fWarp verwalten"), this::openUpdateWarp)
                .build();
        simpleForm.send(player);
    }

    private void openAddWarp(final Player player) {
        final Location location = player.getLocation();
        final CustomForm form = new CustomForm.Builder("§7» §8Warp erstellen")
                .addElement(new ElementInput("§fGebe dem Warp einen Namen.", "Name"))
                .addElement(new ElementInput("§fSetze den Display-Name. Dieser wird in der UI angezeigt."))
                .addElement(new ElementInput("§fLege ein Bild für diesen Warp-Punkt fest, der in der UI angezeigt werden soll."))
                .onSubmit((f, g) -> {
                    final String name = g.getInputResponse(0);
                    final String displayName = g.getInputResponse(1);
                    final String imageUrl = g.getInputResponse(2);

                    if (name.isEmpty() || displayName.isEmpty() || imageUrl.isEmpty()) {
                        player.sendMessage(Language.get("warp.input.invalid"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                                switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                                    case CALLBACK_NULL:
                                        player.sendMessage(Language.get("warp.set", name));
                                        break;
                                    case CALLBACK_WARP_ALREADY_SET:
                                        player.sendMessage(Language.get("warp.already.set"));
                                        break;
                                }
                            }), Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ADD_WARP.name(), name, displayName, imageUrl, this.getPlugin().getServerName(), location.getLevel().getName(),
                            String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
                })
                .build();
        form.send(player);
    }

    private void openUpdateWarp(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (TeleportationCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_ALL_WARPS:
                    final List<String> list = Arrays.asList(delivery.getData());

                    final HashMap<String, Warp> warps = new HashMap<>();
                    final List<String> warpNames = new ArrayList<>();

                    list.forEach(z -> {
                        if (!z.equals(delivery.getKey().toLowerCase())) {
                            final String[] d = z.split(">>");
                            warps.put(d[0], new Warp(d[0], d[1], d[2], d[3], d[4], Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8]), Double.parseDouble(d[9])));
                            warpNames.add(d[0]);
                        }
                    });

                    final CustomForm form = new CustomForm.Builder("§7» §8Warp verwalten")
                            .addElement(new ElementDropdown("§fWähle aus der Liste ein Warp aus, welches du bearbeiten möchtest.", warpNames))
                            .onSubmit((t, z) -> {
                                final String f = z.getDropdownResponse(0).getElementContent();
                                final Warp warp = warps.get(f);

                                if (f.isEmpty()) {
                                    player.sendMessage(Language.get("warp.input.invalid"));
                                    return;
                                }

                                final CustomForm form1 = new CustomForm.Builder("§7» §8Warp verwalten")
                                        .addElement(new ElementLabel("§fDie Einstellungen beziehen sich auf den Warp-Punkt " + f + "§f.\n§cElemente, die nicht bearbeitet werden, können frei gelassen werden."))
                                        .addElement(new ElementInput("§fAnzeige-Namen des Warps ändern.", "", warp.getDisplayName()))
                                        .addElement(new ElementInput("§fBild-Link des Warps ändern.", "", warp.getImageUrl()))
                                        .addElement(new ElementToggle("§fWarp auf meine aktuelle Position aktualisieren.", false))
                                        .addElement(new ElementToggle("§fWarp endgültig entfernen.", false))
                                        .onSubmit((c, v) -> {
                                            final String updateDisplayName = v.getInputResponse(1);
                                            final String updateImageUrl = v.getInputResponse(2);
                                            final boolean updatePosition = v.getToggleResponse(3);
                                            final boolean delete = v.getToggleResponse(4);

                                            if (!delete) {
                                                if (!updateDisplayName.isEmpty()) {
                                                    if (!updateDisplayName.equals(warp.getDisplayName())) {
                                                        this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_RENAME_WARP.name(), warp.getName(), updateDisplayName);
                                                        player.sendMessage(Language.get("warp.update.name", updateDisplayName));
                                                    }
                                                }

                                                if (!updateImageUrl.isEmpty()) {
                                                    if (!updateImageUrl.equals(warp.getImageUrl())) {
                                                        this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_WARP_IMAGE.name(), warp.getName(), updateImageUrl);
                                                        player.sendMessage(Language.get("warp.update.image"));
                                                    }
                                                }

                                                if (updatePosition) {
                                                    final Location location = c.getLocation();
                                                    this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_WARP_POSITION.name(), warp.getName(), this.getPlugin().getServerName(),
                                                            location.getLevel().getName(), String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()),
                                                            String.valueOf(location.getPitch()));
                                                    player.sendMessage(Language.get("warp.update.location"));
                                                }
                                            } else {
                                                this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_WARP.name(), warp.getName());
                                                player.sendMessage(Language.get("warp.deleted", warp.getName()));
                                            }
                                        })
                                        .build();
                                form1.send(t);
                            })
                            .build();
                    form.send(player);
                    break;
            }
        }), Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_WARPS.name());
    }

}