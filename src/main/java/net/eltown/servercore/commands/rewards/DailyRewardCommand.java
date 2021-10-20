package net.eltown.servercore.commands.rewards;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementSlider;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.item.Item;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.rewards.DailyReward;
import net.eltown.servercore.components.data.rewards.RewardCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DailyRewardCommand extends PluginCommand<ServerCore> {

    public DailyRewardCommand(final ServerCore owner) {
        super("dailyreward", owner);
        this.setDescription("Einstellungen für das Reward-System");
        this.setPermission("core.commands.dailyreward");
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

    private void openMain(Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Reward Einstellungen", "")
                .addButton(new ElementButton("§8» §9DailyRewards"), e -> {
                    final CustomForm select = new CustomForm.Builder("§7» §8Reward Einstellungen")
                            .addElement(new ElementSlider("Bitte wähle einen Tag für die täglichen Belohnungen aus.", 1, 14, 1, 1))
                            .onSubmit((g, h) -> {
                                final int i = (int) h.getSliderResponse(0);

                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                                    switch (RewardCalls.valueOf(delivery.getKey().toUpperCase())) {
                                        case CALLBACK_REWARDS:
                                            if (delivery.getData()[1].equals("null")) {
                                                this.openSelectReward(player, i, new ArrayList<>());
                                                return;
                                            }

                                            final List<DailyReward> rewardsToday = new ArrayList<>();
                                            Arrays.asList(delivery.getData()[1].split("-:-")).forEach(p -> {
                                                final String[] l = p.split(">:<");
                                                rewardsToday.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                                            });
                                            this.openSelectReward(player, i, rewardsToday);
                                            break;
                                    }
                                }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(i));
                            })
                            .build();
                    select.send(player);
                })
                .build();
        form.send(player);
    }

    public void openSelectReward(final Player player, final int day, final List<DailyReward> dailyRewards) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Reward Einstellungen", "Bitte wähle einen Reward für den folgenden Tag aus: §9" + day);

        dailyRewards.forEach(e -> {
            form.addButton(new ElementButton(e.getDescription()), g -> {
                final CustomForm editForm = new CustomForm.Builder("§7» §8Reward Einstellungen")
                        .addElement(new ElementInput("§8» §fBeschreibung bearbeiten", e.getDescription(), e.getDescription()))
                        .addElement(new ElementInput("§8» §fWahrscheinlichkeit bearbeiten", "" + e.getChance(), "" + e.getChance()))
                        .addElement(new ElementInput("§8» §fBelohnungs-Daten bearbeiten\n§7xp;<amount>\n§7money;<amount>", e.getData(), e.getData()))
                        .addElement(new ElementToggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false))
                        .addElement(new ElementToggle("§8» §cDiesen Reward löschen", false))
                        .onSubmit((h, j) -> {
                            try {
                                final String description = j.getInputResponse(0);
                                final int chance = Integer.parseInt(j.getInputResponse(1));
                                String data = j.getInputResponse(2);
                                final boolean setItem = j.getToggleResponse(3);
                                final boolean delete = j.getToggleResponse(4);

                                if (description.isEmpty() || data.isEmpty()) {
                                    player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                                    return;
                                }

                                if (!delete) {
                                    if (setItem) {
                                        final Item item = player.getInventory().getItemInHand();
                                        if (item.getId() == 0) {
                                            player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                                            return;
                                        }
                                        data = "item;" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                                    }
                                    player.sendMessage("Der Eintrag wurde aktualisiert.");
                                    this.getPlugin().getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_UPDATE_DAILY_REWARD.name(), description, e.getId(), String.valueOf(day), String.valueOf(chance), data);
                                } else {
                                    player.sendMessage("Der Eintrag wurde gelöscht.");
                                    this.getPlugin().getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_REMOVE_REWARD.name(), e.getId());
                                }
                            } catch (final Exception exception) {
                                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                            }
                        })
                        .build();
                editForm.send(player);
            });
        });

        form.addButton(new ElementButton("§8» §9Neuen Reward eintragen"), e -> {
            final CustomForm addForm = new CustomForm.Builder("§7» §8Reward Einstellungen")
                    .addElement(new ElementInput("§8» §fBeschreibung", "5x Baumstämme"))
                    .addElement(new ElementInput("§8» §fWahrscheinlichkeit", "35"))
                    .addElement(new ElementInput("§8» §fBelohnungs-Daten bearbeiten\n§7xp;<amount>\n§7money;<amount>"))
                    .addElement(new ElementToggle("§8» §fDas Item in meiner Hand als Belohnungs-Daten setzen", false))
                    .onSubmit((h, j) -> {
                        try {
                            final String description = j.getInputResponse(0);
                            final int chance = Integer.parseInt(j.getInputResponse(1));
                            String data = j.getInputResponse(2);
                            final boolean setItem = j.getToggleResponse(3);

                            if (description.isEmpty() || data.isEmpty()) {
                                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                                return;
                            }

                            if (setItem) {
                                final Item item = player.getInventory().getItemInHand();
                                if (item.getId() == 0) {
                                    player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                                    return;
                                }
                                data = "item;" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                            }
                            player.sendMessage("Der Eintrag wurde erstellt.");
                            this.getPlugin().getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_REWARD.name(), description, String.valueOf(day), String.valueOf(chance), data);
                        } catch (final Exception exception) {
                            player.sendMessage("Deine Angaben sind fehlerhaft: Bitte überprüfe diese.");
                        }
                    })
                    .build();
            addForm.send(player);
        });
        form.build().send(player);
    }
}
