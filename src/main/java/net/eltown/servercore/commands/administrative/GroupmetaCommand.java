package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.groupmanager.Group;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GroupmetaCommand extends PluginCommand<ServerCore> {

    public GroupmetaCommand(final ServerCore owner) {
        super("groupmeta", owner);
        this.setDescription("Groupmeta Command");
        this.setPermission("core.command.groupmeta");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final CustomForm customForm = new CustomForm.Builder("§7» §8Meta bearbeiten")
                    .addElement(new ElementDropdown("§8» §7Gruppe", this.getGroups()))
                    .onSubmit((e, r) -> {
                        this.openMenu((Player) sender, r.getDropdownResponse(0).getElementContent());
                    })
                    .build();
            customForm.send((Player) sender);
        }
        return true;
    }

    private void openMenu(final Player player, final String group) {
        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GROUP_DOES_NOT_EXIST:
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
