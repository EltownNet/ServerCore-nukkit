package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementSlider;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.guardian.GuardianCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;

public class MuteCommand extends PluginCommand<ServerCore> {

    public MuteCommand(final ServerCore owner) {
        super("mute", owner);
        this.setDescription("Mute Command");
        this.setPermission("core.command.mute");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Muten")
                    .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname", input))
                    .addElement(new ElementInput("§8» §7Grund der Bestrafung", "Grund"))
                    .addElement(new ElementDropdown("§8» §7Auswahl der Zeiteinheit", Arrays.asList("m", "h", "d", "M"), 2))
                    .addElement(new ElementSlider("§8» §7Auswahl der Zeit", 1, 100, 1, 3))
                    .onSubmit((e, r) -> {
                        final String target = r.getInputResponse(0);
                        final String reason = r.getInputResponse(1);
                        final long duration = this.getPlugin().getDuration(r.getDropdownResponse(2).getElementContent(), (int) r.getSliderResponse(3));

                        if (target.isEmpty() || reason.isEmpty()) {
                            sender.sendMessage(Language.get("guardian.invalid.input"));
                            return;
                        }

                        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                            switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_MUTE_IS_MUTED:
                                    sender.sendMessage(Language.get("guardian.mute.is.muted", target));
                                    break;
                                case CALLBACK_MUTE_EXECUTED:
                                    sender.sendMessage(Language.get("guardian.mute.executed", target, reason, delivery.getData()[1]));
                                    break;
                            }
                        }), Queue.GUARDIAN, GuardianCalls.REQUEST_INITIATE_MUTE.name(), target, reason, sender.getName(), String.valueOf(duration));
                    })
                    .build();
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
