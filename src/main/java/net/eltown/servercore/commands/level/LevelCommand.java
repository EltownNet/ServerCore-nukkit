package net.eltown.servercore.commands.level;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

public class LevelCommand extends PluginCommand<ServerCore> {

    public LevelCommand(final ServerCore owner) {
        super("level", owner);
        this.setDescription("Lasse dir deinen Level-Fortschritt anzeigen");
        this.setPermission("core.commands.level");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openMain(player);
        }
        return true;
    }

    private void openMain(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Level Einstellungen", "Bitte wähle eine Kategorie aus, um fortzufahren.")
                .addButton(new ElementButton("§8» §9Rewards"), this::openUpdateReward)
                .build();
        form.send(player);
    }

    private void openUpdateReward(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Level Reward Einstellungen")
                .addElement(new ElementInput("Bitte gebe ein Level an.", "12"))
                .onSubmit((g, h) -> {
                    try {
                        final int i = Integer.parseInt(h.getInputResponse(0));
                        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                            switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_LEVEL_REWARD:
                                    final String levelReward = delivery.getData()[1];
                                    final CustomForm customForm = new CustomForm.Builder("§7» §8Level Reward Einstellungen")
                                            .addElement(new ElementInput("Folgender Reward ist bereits hinterlegt:", "Reward", levelReward))
                                            .onSubmit((k, l) -> {
                                                final String update = l.getInputResponse(0);
                                                if (!update.isEmpty()) {
                                                    this.getPlugin().getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_REWARD.name(), String.valueOf(i), update);
                                                    player.sendMessage(Language.get("level.settings.updated"));
                                                } else player.sendMessage(Language.get("level.settings.invalid.input"));
                                            })
                                            .build();
                                    customForm.send(player);
                                    break;
                                case CALLBACK_NULL:
                                    final CustomForm reward = new CustomForm.Builder("§7» §8Level Reward Einstellungen")
                                            .addElement(new ElementInput("Für dieses Level existiert noch kein Reward. Bitte hinterlege einen.", "Reward"))
                                            .onSubmit((k, l) -> {
                                                final String update = l.getInputResponse(0);
                                                if (!update.isEmpty()) {
                                                    this.getPlugin().getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_REWARD.name(), String.valueOf(i), update);
                                                    player.sendMessage(Language.get("level.settings.created"));
                                                } else player.sendMessage(Language.get("level.settings.invalid.input"));
                                            })
                                            .build();
                                    reward.send(player);
                                    break;
                            }
                        }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_LEVEL_REWARD.name(), String.valueOf(i));
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("level.settings.invalid.input"));
                    }
                })
                .build();
        form.send(player);
    }
}
