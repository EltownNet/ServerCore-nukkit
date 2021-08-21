package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.groupmanager.GroupedPlayer;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GroupCommand extends PluginCommand<ServerCore> {

    public GroupCommand(final ServerCore owner) {
        super("group", owner);
        this.setDescription("Group Command");
        this.setPermission("core.command.group");
        this.setAliases(Arrays.asList("setgroup", "rank").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final SimpleForm form = new SimpleForm.Builder("§7» §8Gruppeneinstellungen", "Bitte wähle eine Kategorie aus.")
                    .addButton(new ElementButton("§8» §9Gruppe bearbeiten"), e -> {
                        final CustomForm customForm = new CustomForm.Builder("§7» §8Gruppe bearbeiten")
                                .addElement(new ElementDropdown("§8» §7Gruppe auswählen", this.getGroups()))
                                .onSubmit((g, r) -> {
                                    this.openMenu((Player) sender, r.getDropdownResponse(0).getElementContent());
                                })
                                .build();
                        customForm.send((Player) sender);
                    })
                    .addButton(new ElementButton("§8» §9Spieler bearbeiten"), e -> {
                        final CustomForm customForm = new CustomForm.Builder("§7» §8Spieler bearbeiten")
                                .addElement(new ElementInput("§8» §7Spieler auswählen", "Nickname"))
                                .onSubmit((g, r) -> {
                                    if (!r.getInputResponse(0).isEmpty()) {
                                        this.openPlayerMenu((Player) sender, r.getInputResponse(0));
                                    } else sender.sendMessage(Language.get("group.invalid.input"));
                                })
                                .build();
                        customForm.send((Player) sender);
                    })
                    .addButton(new ElementButton("§8» §9Gruppe setzen"), this::setGroup)
                    .addButton(new ElementButton("§8» §9Gruppe erstellen"), this::createGroup)
                    .build();
            form.send((Player) sender);
        }
        return true;
    }

    private void openPlayerMenu(final Player player, final String target) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP_PLAYER:
                    final GroupedPlayer groupedPlayer = new GroupedPlayer(target, delivery.getData()[1], Long.parseLong(delivery.getData()[2]), Arrays.asList(delivery.getData()[6].split("#")));
                    final SimpleForm simpleForm = new SimpleForm.Builder("§7» §8Spieler bearbeiten", "§fGruppe: §7" + groupedPlayer.getGroup())
                            .addButton(new ElementButton("§8» §9Berechtigung hinzufügen"), e -> this.addPlayerPermission(e, groupedPlayer))
                            .addButton(new ElementButton("§8» §9Berechtigung entfernen"), e -> this.removePlayerPermission(e, groupedPlayer))
                            .build();
                    simpleForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP_PLAYER.name(), target);
    }

    private void openMenu(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_PLAYER_DOES_NOT_EXIST:
                    player.sendMessage(Language.get("group.not.exists", group));
                    break;
                case CALLBACK_NULL:
                    final SimpleForm simpleForm = new SimpleForm.Builder("§7» §8Meta bearbeiten", "§fGruppe: §7" + group)
                            .addButton(new ElementButton("§8» §9Berechtigung hinzufügen"), e -> this.addPermission(e, group))
                            .addButton(new ElementButton("§8» §9Berechtigung entfernen"), e -> this.removePermission(e, group))
                            .addButton(new ElementButton("§8» §9Vererbung hinzufügen"), e -> this.addInheritance(e, group))
                            .addButton(new ElementButton("§8» §9Vererbung entfernen"), e -> this.removeInheritance(e, group))
                            .addButton(new ElementButton("§8» §9Prefix ändern"), e -> this.changePrefix(e, group))
                            .build();
                    simpleForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_GROUP_EXISTS.name(), group);
    }

    private void addPermission(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP:
                    final List<String> permissions = Arrays.asList(delivery.getData()[2].split("#"));

                    final CustomForm customForm = new CustomForm.Builder("§7» §8Berechtigung hinzufügen")
                            .addElement(new ElementInput("§8» §7Berechtigungsschlüssel", "plugin.key.subkey"))
                            .onSubmit((e, r) -> {
                                final String key = r.getInputResponse(0);

                                if (key.isEmpty()) {
                                    player.sendMessage(Language.get("group.invalid.input"));
                                    return;
                                }
                                if (permissions.contains(key)) {
                                    player.sendMessage(Language.get("group.group.permission.already.added", key));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_SUCCESS:
                                            player.sendMessage(Language.get("group.group.permission.added", group, key));
                                            PluginCommand.broadcastCommandMessage(player, "Added permission " + key + " to group " + group, false);
                                            break;
                                    }
                                }), Queue.GROUPS, GroupCalls.REQUEST_ADD_PERMISSION.name(), group, key);
                            })
                            .build();
                    customForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP.name(), group);
    }

    private void addPlayerPermission(final Player player, final GroupedPlayer target) {
        final CustomForm customForm = new CustomForm.Builder("§7» §8Berechtigung hinzufügen")
                .addElement(new ElementInput("§8» §7Berechtigungsschlüssel", "plugin.key.subkey"))
                .onSubmit((e, r) -> {
                    final String key = r.getInputResponse(0);

                    if (key.isEmpty()) {
                        player.sendMessage(Language.get("group.invalid.input"));
                        return;
                    }
                    if (target.getPermissions().contains(key)) {
                        player.sendMessage(Language.get("group.player.permission.already.added", key));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                        switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                            case CALLBACK_SUCCESS:
                                player.sendMessage(Language.get("group.player.permission.added", player.getName(), key));
                                PluginCommand.broadcastCommandMessage(player, "Added permission " + key + " to user " + player.getName(), false);
                                break;
                        }
                    }), Queue.GROUPS, GroupCalls.REQUEST_ADD_PLAYER_PERMISSION.name(), target.getPlayer(), key);
                })
                .build();
        customForm.send(player);
    }

    private void removePermission(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP:
                    final List<String> permissions = Arrays.asList(delivery.getData()[3].split("#"));

                    final CustomForm customForm = new CustomForm.Builder("§7» §8Berechtigung entfernen")
                            .addElement(new ElementDropdown("§8» §7Berechtigungsschlüssel", permissions))
                            .onSubmit((e, r) -> {
                                final String key = r.getDropdownResponse(0).getElementContent();

                                if (key.isEmpty()) {
                                    player.sendMessage(Language.get("group.invalid.input"));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_SUCCESS:
                                            player.sendMessage(Language.get("group.group.permission.removed", group, key));
                                            PluginCommand.broadcastCommandMessage(player, "Removed permission " + key + " from group " + group, false);
                                            break;
                                    }
                                }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_PERMISSION.name(), group, key);
                            })
                            .build();
                    customForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP.name(), group);
    }

    private void removePlayerPermission(final Player player, final GroupedPlayer target) {
        final CustomForm customForm = new CustomForm.Builder("§7» §8Berechtigung entfernen")
                .addElement(new ElementDropdown("§8» §7Berechtigungsschlüssel", target.getPermissions()))
                .onSubmit((e, r) -> {
                    final String key = r.getDropdownResponse(0).getElementContent();

                    if (key.isEmpty()) {
                        player.sendMessage(Language.get("group.invalid.input"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                        switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                            case CALLBACK_SUCCESS:
                                player.sendMessage(Language.get("group.player.permission.removed", player.getName(), key));
                                PluginCommand.broadcastCommandMessage(player, "Removed permission " + key + " from user " + player.getName(), false);
                                break;
                        }
                    }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_PLAYER_PERMISSION.name(), target.getPlayer(), key);
                })
                .build();
        customForm.send(player);
    }

    private void addInheritance(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP:
                    final List<String> inheritances = Arrays.asList(delivery.getData()[3].split("#"));

                    final CustomForm customForm = new CustomForm.Builder("§7» §8Vererbung hinzufügen")
                            .addElement(new ElementDropdown("§8» §7Gruppe", this.getGroups()))
                            .onSubmit((e, r) -> {
                                final String key = r.getDropdownResponse(0).getElementContent();

                                if (key.isEmpty()) {
                                    player.sendMessage(Language.get("group.invalid.input"));
                                    return;
                                }
                                if (inheritances.contains(key)) {
                                    player.sendMessage(Language.get("group.group.inheritance.already.added", key));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_SUCCESS:
                                            player.sendMessage(Language.get("group.group.inheritance.added", group, key));
                                            PluginCommand.broadcastCommandMessage(player, "Added inheritance " + key + " to group " + group, false);
                                            break;
                                        case CALLBACK_GROUP_DOES_NOT_EXIST:
                                            player.sendMessage(Language.get("group.group.not.exists", group));
                                            break;
                                    }
                                }), Queue.GROUPS, GroupCalls.REQUEST_ADD_INHERITANCE.name(), group, key);
                            })
                            .build();
                    customForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP.name(), group);
    }

    private void removeInheritance(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP:
                    final List<String> inheritances = Arrays.asList(delivery.getData()[4].split("#"));

                    final CustomForm customForm = new CustomForm.Builder("§7» §8Vererbung entfernen")
                            .addElement(new ElementDropdown("§8» §7Gruppe", inheritances))
                            .onSubmit((e, r) -> {
                                final String key = r.getDropdownResponse(0).getElementContent();

                                if (key.isEmpty()) {
                                    player.sendMessage(Language.get("group.invalid.input"));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_SUCCESS:
                                            player.sendMessage(Language.get("group.group.inheritance.removed", group, key));
                                            PluginCommand.broadcastCommandMessage(player, "Removed inheritance " + key + " from group " + group, false);
                                            break;
                                        case CALLBACK_GROUP_DOES_NOT_EXIST:
                                            player.sendMessage(Language.get("group.group.not.exists", group));
                                            break;
                                    }
                                }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_INHERITANCE.name(), group, key);
                            })
                            .build();
                    customForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP.name(), group);
    }

    private void changePrefix(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FULL_GROUP:
                    final String prefix = delivery.getData()[2];

                    final CustomForm customForm = new CustomForm.Builder("§7» §8Prefix ändern")
                            .addElement(new ElementInput("§8» §7Prefix", "Prefix", prefix))
                            .onSubmit((e, r) -> {
                                final String key = r.getInputResponse(0);

                                if (key.isEmpty()) {
                                    player.sendMessage(Language.get("group.invalid.input"));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                                    switch (GroupCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_SUCCESS:
                                            player.sendMessage(Language.get("group.group.prefix.changed", group, key));
                                            PluginCommand.broadcastCommandMessage(player, "Changed prefix to " + key + " for group " + group, false);
                                            break;
                                    }
                                }), Queue.GROUPS, GroupCalls.REQUEST_CHANGE_PREFIX.name(), group, key);
                            })
                            .build();
                    customForm.send(player);
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP.name(), group);
    }

    private void setGroup(final Player sender) {
        final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Gruppe Setzen")
                .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname"))
                .addElement(new ElementDropdown("§8» §7Name der Gruppe", this.getGroups()))
                .addElement(new ElementDropdown("§8» §7Auswahl der Zeiteinheit", Arrays.asList("m", "h", "d", "M", "Permanent"), 2))
                .addElement(new ElementSlider("§8» §7Auswahl der Zeit", 1, 100, 1, 3))
                .onSubmit((e, r) -> {
                    final String target = r.getInputResponse(0);
                    final String group = r.getDropdownResponse(1).getElementContent();
                    final long duration = this.getPlugin().getDuration(r.getDropdownResponse(2).getElementContent(), (int) r.getSliderResponse(3));

                    if (target.isEmpty() || group.isEmpty()) {
                        sender.sendMessage(Language.get("group.invalid.input"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                        switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_GROUP_DOES_NOT_EXIST:
                                sender.sendMessage(Language.get("group.group.not.exists", group));
                                break;
                            case CALLBACK_PLAYER_ALREADY_IN_GROUP:
                                sender.sendMessage(Language.get("group.player.already.in.group", target, group));
                                break;
                            case CALLBACK_PLAYER_DOES_NOT_EXIST:
                                sender.sendMessage(Language.get("group.player.not.exists", target));
                                break;
                            case CALLBACK_SUCCESS:
                                sender.sendMessage(Language.get("group.player.group.set", target, group, this.getPlugin().getRemainingTimeFuture(duration)));
                                PluginCommand.broadcastCommandMessage(sender, "Set group " + group + " to " + target + " for " + this.getPlugin().getRemainingTimeFuture(duration), false);
                                break;
                        }
                    }), Queue.GROUPS, GroupCalls.REQUEST_SET_GROUP.name(), target, group, sender.getName(), String.valueOf(duration));
                })
                .build();
        formWindowCustom.send(sender);
    }

    private void createGroup(final Player sender) {
        final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Gruppe Erstellen")
                .addElement(new ElementInput("§8» §7Name der Gruppe", "Gruppe", "GROUP"))
                .addElement(new ElementInput("§8» §7Prefix der Gruppe", "Prefix"))
                .onSubmit((e, r) -> {
                    final String group = r.getInputResponse(0);
                    final String prefix = r.getInputResponse(1);

                    if (group.isEmpty() || prefix.isEmpty()) {
                        sender.sendMessage(Language.get("group.invalid.input"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                        switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_GROUP_ALREADY_EXIST:
                                sender.sendMessage(Language.get("group.group.already.exists", group));
                                break;
                            case CALLBACK_SUCCESS:
                                sender.sendMessage(Language.get("group.group.created", group));
                                PluginCommand.broadcastCommandMessage(sender, "Created group " + group, false);
                                break;
                        }
                    }), Queue.GROUPS, GroupCalls.REQUEST_CREATE_GROUP.name(), group, prefix, sender.getName());
                })
                .build();
        formWindowCustom.send(sender);
    }

    private List<String> getGroups() {
        final AtomicReference<List<String>> list = new AtomicReference<>();
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GROUPS:
                    list.set(Arrays.asList(delivery.getData()[1].split("#")));
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_GROUPS.name());
        return list.get();
    }

}
