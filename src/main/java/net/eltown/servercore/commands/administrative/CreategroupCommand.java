package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementInput;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

public class CreategroupCommand extends PluginCommand<ServerCore> {

    public CreategroupCommand(final ServerCore owner) {
        super("creategroup", owner);
        this.setDescription("Creategroup Command");
        this.setPermission("core.command.creategroup");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Gruppe Erstellen")
                    .addElement(new ElementInput("§8» §7Name der Gruppe", "Gruppe", input))
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
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
