package net.eltown.servercore.commands.level;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.item.Item;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.level.LevelReward;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

public class LevelCommand extends PluginCommand<ServerCore> {

    public LevelCommand(final ServerCore owner) {
        super("level", owner);
        this.setDescription("Einstellungen für das LevelSystem");
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
                                    final LevelReward levelReward = new LevelReward(Integer.parseInt(delivery.getData()[1]), delivery.getData()[2], delivery.getData()[3]);
                                    final CustomForm customForm = new CustomForm.Builder("§7» §8Level Reward Einstellungen")
                                            .addElement(new ElementLabel("§8» §fLevel: §9" + i))
                                            .addElement(new ElementInput("§8» §fBeschreibung", "Beschreibung", levelReward.getDescription()))
                                            .addElement(new ElementInput("§8» §fBelohnungs-Daten\n§7AllgemeinesTrennzeichen: #\n§7Gutschein-Belohnungs-Trennzeichen: >:<", levelReward.getData(), levelReward.getData()))
                                            .addElement(new ElementToggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false))
                                            .addElement(new ElementToggle("§8» §cDiesen Reward löschen", false))
                                            .onSubmit((k, l) -> {
                                                final String description = l.getInputResponse(1);
                                                String data = l.getInputResponse(2);
                                                final boolean setItem = l.getToggleResponse(3);
                                                final boolean delete = l.getToggleResponse(4);

                                                if (description.isEmpty()) {
                                                    player.sendMessage(Language.get("level.settings.invalid.input"));
                                                    return;
                                                }

                                                if (!delete) {
                                                    if (setItem) {
                                                        final Item item = player.getInventory().getItemInHand();
                                                        if (item.getId() == 0) {
                                                            player.sendMessage(Language.get("level.settings.invalid.input"));
                                                            return;
                                                        }
                                                        data = "item#" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                                                    }
                                                    if (data.isEmpty()) data = "null";

                                                    player.sendMessage(Language.get("level.settings.updated"));
                                                    this.getPlugin().getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_REWARD.name(), String.valueOf(i), description, data);
                                                } else {
                                                    player.sendMessage(Language.get("level.settings.updated"));
                                                    this.getPlugin().getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_REMOVE_REWARD.name(), String.valueOf(i));
                                                }
                                            })
                                            .build();
                                    customForm.send(player);
                                    break;
                                case CALLBACK_NULL:
                                    final CustomForm createFrom = new CustomForm.Builder("§7» §8Level Reward Einstellungen")
                                            .addElement(new ElementLabel("§8» §fLevel: §9" + i))
                                            .addElement(new ElementInput("§8» §fBeschreibung", "Beschreibung"))
                                            .addElement(new ElementInput("§8» §fBelohnungs-Daten\n§7AllgemeinesTrennzeichen: #\n§7Gutschein-Belohnungs-Trennzeichen: >:<"))
                                            .addElement(new ElementToggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false))
                                            .onSubmit((k, l) -> {
                                                final String description = l.getInputResponse(1);
                                                String data = l.getInputResponse(2);
                                                final boolean setItem = l.getToggleResponse(3);

                                                if (description.isEmpty()) {
                                                    player.sendMessage(Language.get("level.settings.invalid.input"));
                                                    return;
                                                }

                                                if (setItem) {
                                                    final Item item = player.getInventory().getItemInHand();
                                                    if (item.getId() == 0) {
                                                        player.sendMessage(Language.get("level.settings.invalid.input"));
                                                        return;
                                                    }
                                                    data = "item#" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                                                }
                                                if (data.isEmpty()) data = "null";

                                                player.sendMessage(Language.get("level.settings.updated"));
                                                this.getPlugin().getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_REWARD.name(), String.valueOf(i), description, data);
                                            })
                                            .build();
                                    createFrom.send(player);
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
