package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementInput;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.guardian.GuardianCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

public class UnmuteCommand extends PluginCommand<ServerCore> {

    public UnmuteCommand(final ServerCore owner) {
        super("unmute", owner);
        this.setDescription("Unmute Command");
        this.setPermission("core.command.unmute");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Unmute")
                    .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname", input))
                    .addElement(new ElementInput("§8» §7Grund der Aufhebung", "Grund"))
                    .onSubmit((e, r) -> {
                        final String target = r.getInputResponse(0);
                        final String reason = r.getInputResponse(1);

                        if (target.isEmpty() || reason.isEmpty()) {
                            sender.sendMessage(Language.get("guardian.invalid.input"));
                            return;
                        }

                        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                            switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_MUTE_IS_NOT_MUTED:
                                    sender.sendMessage(Language.get("guardian.mute.unmute.not.muted", target));
                                    break;
                                case CALLBACK_MUTE_CANCELLED:
                                    sender.sendMessage(Language.get("guardian.mute.unmute.executed", target, reason, delivery.getData()[1]));
                                    break;
                            }
                        }), Queue.GUARDIAN, GuardianCalls.REQUEST_CANCEL_MUTE.name(), target, reason, sender.getName());
                    })
                    .build();
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
