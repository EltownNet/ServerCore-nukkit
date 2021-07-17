package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementSlider;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SetgroupCommand extends PluginCommand<ServerCore> {

    public SetgroupCommand(final ServerCore owner) {
        super("setgroup", owner);
        this.setDescription("Setgroup Command");
        this.setPermission("core.command.setgroup");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Gruppe Setzen")
                    .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname", input))
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
            formWindowCustom.send((Player) sender);
        }
        return true;
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
