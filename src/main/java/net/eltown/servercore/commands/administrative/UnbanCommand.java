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

public class UnbanCommand extends PluginCommand<ServerCore> {

    public UnbanCommand(final ServerCore owner) {
        super("unban", owner);
        this.setDescription("Unban Command");
        this.setPermission("core.command.unban");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Unban")
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
                                case CALLBACK_BAN_IS_NOT_BANNED:
                                    sender.sendMessage(Language.get("guardian.ban.unban.not.banned", target));
                                    break;
                                case CALLBACK_BAN_CANCELLED:
                                    sender.sendMessage(Language.get("guardian.ban.unban.executed", target, reason, delivery.getData()[1]));
                                    break;
                            }
                        }), Queue.GUARDIAN, GuardianCalls.REQUEST_CANCEL_BAN.name(), target, reason, sender.getName());
                    })
                    .build();
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
