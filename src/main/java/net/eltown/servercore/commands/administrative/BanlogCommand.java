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

import java.util.*;

public class BanlogCommand extends PluginCommand<ServerCore> {

    public BanlogCommand(final ServerCore owner) {
        super("banlog", owner);
        this.setDescription("Banlog Command");
        this.setPermission("core.command.banlog");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            String input = "";
            if (args.length >= 1) input = args[0];

            final CustomForm formWindowCustom = new CustomForm.Builder("§7» §8Banlog")
                    .addElement(new ElementInput("§8» §7Nickname des Spielers", "Nickname", input))
                    .onSubmit((e, r) -> {
                        final String target = r.getInputResponse(0);

                        if (target.isEmpty()) {
                            sender.sendMessage(Language.get("guardian.invalid.input"));
                            return;
                        }

                        this.getPlugin().getTinyRabbit().sendAndReceive((delivery -> {
                            switch (GuardianCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_ENTRIES_NOT_FOUND:
                                    sender.sendMessage(Language.get("guardian.ban.log.no.entries"));
                                    break;
                                case CALLBACK_BAN_BEHAVIOR_ENTRIES_RECEIVED:
                                    final List<String> list = Arrays.asList(delivery.getData());

                                    final StringBuilder content = new StringBuilder("§fAuszug von: §7" + target + "\n§fDatensätze: §7" + (list.size() - 1) + "\n\n");
                                    list.forEach(z -> {
                                        if (!z.equals(delivery.getKey().toLowerCase())) {
                                            final String[] d = z.split(">>");
                                            content.append("§eBanID: §7").append(d[1]).append("\n").append("§eLogID: §7").append(d[0]).append("\n").append("§eGrund: §7")
                                                    .append(d[3]).append("\n").append("§eErsteller: §7").append(d[4]).append("\n").append("§eDatum: §7").append(d[5]).append("\n")
                                                    .append("§eLänge: §7").append(this.getPlugin().getRemainingTimeFuture((Long.parseLong(d[6]) - Long.parseLong(d[7])) + System.currentTimeMillis())).append("\n\n");
                                        }
                                    });

                                    final SimpleForm form = new SimpleForm.Builder("§7» §8Banlog", content.toString()).build();
                                    form.send(e);
                                    break;
                            }
                        }), Queue.GUARDIAN, GuardianCalls.REQUEST_GET_BAN_BEHAVIOR_ENTRIES.name(), target);
                    })
                    .build();
            formWindowCustom.send((Player) sender);
        }
        return true;
    }
}
