package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementInput;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.guardian.GuardianCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;

public class CheckbanCommand extends PluginCommand<ServerCore> {

    public CheckbanCommand(final ServerCore owner) {
        super("checkban", owner);
        this.setDescription("Checkban Command");
        this.setPermission("core.command.checkban");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Checkban")
                    .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname", input))
                    .onSubmit((e, r) -> {
                        final String target = r.getInputResponse(0);

                        if (target.isEmpty()) {
                            sender.sendMessage(Language.get("guardian.invalid.input"));
                            return;
                        }

                        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                            switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_BAN_IS_NOT_BANNED:
                                    sender.sendMessage(Language.get("guardian.ban.is.not.banned", target));
                                    break;
                                case CALLBACK_ACTIVE_BAN_ENTRY_RECEIVED:
                                    final List<String> list = Arrays.asList(delivery.getData());

                                    final StringBuilder content = new StringBuilder("§fAuszug von: §7" + target + "\n\n");
                                    list.forEach(z -> {
                                        if (!z.equals(delivery.getKey().toLowerCase())) {
                                            final String[] d = z.split(">>");
                                            content.append("§eBanID: §7").append(d[0]).append("\n").append("§eGrund: §7")
                                                    .append(d[2]).append("\n").append("§eErsteller: §7").append(d[3]).append("\n").append("§eDatum: §7").append(d[4]).append("\n")
                                                    .append("§eVerbleibend: §7").append(this.getPlugin().getRemainingTimeFuture(Long.parseLong(d[5]))).append("\n\n");
                                        }
                                    });

                                    final SimpleForm form = new SimpleForm.Builder("§7» §8Checkban", content.toString()).build();
                                    form.send(e);
                                    break;
                            }
                        }), Queue.GUARDIAN, GuardianCalls.REQUEST_GET_BAN_ENTRY_TARGET.name(), target);
                    })
                    .build();
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
