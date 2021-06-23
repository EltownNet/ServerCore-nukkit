package net.eltown.servercore.commands.teleportation;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.Home;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HomeCommand extends PluginCommand<ServerCore> {

    public HomeCommand(ServerCore owner) {
        super("home", owner);
        this.setDescription("Erstelle und verwalte deine Homes");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openMain(player);
        }
        return true;
    }

    private void openMain(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Homes", "Bitte wähle eine Aktion, mit der du fortfahren möchtest.");
        form.addButton(new ElementButton("§7» §fZum Home teleportieren"), this::openTeleporter);
        form.addButton(new ElementButton("§7» §fHome erstellen"), this::openCreator);
        form.addButton(new ElementButton("§7» §fHomes verwalten"), this::openSettings);
        if (player.isOp()) form.addButton(new ElementButton("§7» §4Server verwalten"), this::openAdminSettings);
        form.build().send(player);
    }

    public void openTeleporter(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive((data) -> {
            switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    player.sendMessage(Language.get("home.no.homes"));
                    break;
                case CALLBACK_ALL_HOMES:
                    final List<String> list = Arrays.asList(data.getData());

                    final HashMap<String, Home> homes = new HashMap<>();
                    final List<String> homeNames = new ArrayList<>();

                    list.forEach(z -> {
                        if (!z.equals(data.getKey().toLowerCase())) {
                            final String[] d = z.split(">>");
                            homes.put(d[0], new Home(d[0], d[1], d[2], d[3], Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8])));
                            homeNames.add(d[0]);
                        }
                    });

                    final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Teleportation", "§fWähle aus der Liste ein Home aus, zu welchem du dich teleportieren möchtest.");
                    homeNames.forEach(e -> {
                        form.addButton(new ElementButton(e), l -> {
                            final Home home = homes.get(e);
                            if (home.getServer().equals(this.getPlugin().getServerName())) {
                                player.teleport(new Location(home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch(), this.getPlugin().getServer().getLevelByName(home.getWorld())));
                                player.sendMessage(Language.get("home.teleported", home.getName()));
                            } else {
                                this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_TELEPORT.name(), home.getName(), home.getPlayer(), home.getServer(), home.getWorld(),
                                        String.valueOf(home.getX()), String.valueOf(home.getY()), String.valueOf(home.getZ()), String.valueOf(home.getYaw()), String.valueOf(home.getPitch()));
                            }
                        });
                    });

                    form.build().send(player);
                    break;
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_HOMES.name(), player.getName());
    }

    public void openCreator(final Player player) {
        final Location location = player.getLocation();
        final CustomForm form = new CustomForm.Builder("§7» §8Home erstellen")
                .addElement(new ElementInput("§fBitte gebe deinem Home einen Namen.", "Name"))
                .onSubmit((e, r) -> {
                    final String g = r.getInputResponse(0);

                    if (g.isEmpty() || g.contains("/")) {
                        player.sendMessage(Language.get("home.input.invalid"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((data) -> {
                                switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                                    case CALLBACK_NULL:
                                        player.sendMessage(Language.get("home.set", g));
                                        break;
                                    case CALLBACK_HOME_ALREADY_SET:
                                        player.sendMessage(Language.get("home.already.set"));
                                        break;
                                }
                            }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ADD_HOME.name(), g, player.getName(), this.getPlugin().getServerName(), location.getLevel().getName(),
                            String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
                })
                .build();
        form.send(player);
    }

    public void openSettings(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive((data) -> {
            switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    player.sendMessage(Language.get("home.no.homes"));
                    break;
                case CALLBACK_ALL_HOMES:
                    final List<String> list = Arrays.asList(data.getData());

                    final HashMap<String, Home> homes = new HashMap<>();
                    final List<String> homeNames = new ArrayList<>();

                    list.forEach(z -> {
                        if (!z.equals(data.getKey().toLowerCase())) {
                            final String[] d = z.split(">>");
                            homes.put(d[0], new Home(d[0], d[1], d[2], d[3], Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), Double.parseDouble(d[8])));
                            homeNames.add(d[0]);
                        }
                    });

                    final CustomForm form = new CustomForm.Builder("§7» §8Einstellungen")
                            .addElement(new ElementDropdown("§fWähle aus der Liste ein Home aus, welches du bearbeiten möchtest.", homeNames))
                            .onSubmit((e, r) -> {
                                final String f = r.getDropdownResponse(0).getElementContent();

                                if (f.isEmpty()) {
                                    player.sendMessage(Language.get("home.input.invalid"));
                                    return;
                                }

                                final CustomForm form1 = new CustomForm.Builder("§7» §8Einstellungen")
                                        .addElement(new ElementLabel("§fDie Einstellungen beziehen sich auf das Home §f" + f))
                                        .addElement(new ElementInput("§7Wenn du den Namen nicht ändern möchtest, dann lasse dieses Feld frei.", "Name"))
                                        .addElement(new ElementToggle("Möchtest du die Home-Position ändern?", false))
                                        .addElement(new ElementToggle("Möchtest du dein Home endgültig löschen?", false))
                                        .onSubmit((g, h) -> {
                                            final String i = h.getInputResponse(1);
                                            final boolean o = h.getToggleResponse(2);
                                            final boolean v = h.getToggleResponse(3);

                                            if (!v) {
                                                if (!i.isEmpty()) {
                                                    if (!i.contains("/")) {
                                                        this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_RENAME_HOME.name(), f, g.getName(), i);
                                                        player.sendMessage(Language.get("home.renamed", i));
                                                    } else player.sendMessage(Language.get("home.input.invalid"));
                                                }

                                                if (o) {
                                                    final Location location = g.getLocation();
                                                    this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_UPDATE_POSITION.name(), f, g.getName(), this.getPlugin().getServerName(), location.getLevel().getName(),
                                                            String.valueOf(location.getX()), String.valueOf(location.getY()), String.valueOf(location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()));
                                                    player.sendMessage(Language.get("home.updated"));
                                                }
                                            } else {
                                                this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_HOME.name(), f, g.getName());
                                                player.sendMessage(Language.get("home.deleted", f));
                                            }
                                        })
                                        .build();
                                form1.send(e);
                            })
                            .build();
                    form.send(player);

                    break;
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_ALL_HOMES.name(), player.getName());
    }

    private void openAdminSettings(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Admin Einstellungen", "§cSobald du auf einen Button klickst, wird die besagte Aktion sofort ausgeführt.\n§cServer: §7" + this.getPlugin().getServerName())
                .addButton(new ElementButton("§7» §4Alle Homes dieses\n§4Servers entfernen"), e -> {
                    this.getPlugin().getTinyRabbit().send(Queue.TELEPORTATION_RECEIVE, TeleportationCalls.REQUEST_DELETE_ALL_SERVER_HOMES.name(), this.getPlugin().getServerName());
                    player.sendMessage(Language.get("home.server.deleted"));
                })
                .build();
        form.send(player);
    }

}
