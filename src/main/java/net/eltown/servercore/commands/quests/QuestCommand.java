package net.eltown.servercore.commands.quests;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.FullQuestPlayer;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class QuestCommand extends PluginCommand<ServerCore> {

    public QuestCommand(final ServerCore owner) {
        super("quest", owner);
        this.setDescription("Sehe deine aktuellen Quests ein.");
        this.setAliases(Arrays.asList("quests", "aufgaben").toArray(new String[]{}));
    }

    private final HashMap<String, Location> pos1 = new HashMap<>();
    private final HashMap<String, Location> pos2 = new HashMap<>();

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length == 1 && args[0].equalsIgnoreCase("pos1") && player.isOp()) {
                this.pos1.put(player.getName(), player.getLocation());
                player.sendMessage("Position 1 wurde gesetzt.");
            } else if (args.length == 1 && args[0].equalsIgnoreCase("pos2") && player.isOp()) {
                this.pos2.put(player.getName(), player.getLocation());
                player.sendMessage("Position 2 wurde gesetzt.");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("take") && player.isOp()) {
                final String questNameId = args[1];
                this.getPlugin().getQuestAPI().getQuest(questNameId, quest -> {
                    if (quest == null) {
                        player.sendMessage("Diese Quest existiert nicht.");
                        return;
                    }
                    if (!this.getPlugin().getQuestAPI().playerIsInQuest(player.getName(), questNameId)) {
                        this.getPlugin().getQuestAPI().setQuestOnPlayer(player.getName(), questNameId);
                        player.sendMessage("Du hast die Quest " + quest.getDisplayName() + " §rangenommen.");
                        player.sendMessage(quest.getDescription());
                    } else {
                        this.getPlugin().getQuestAPI().removeQuestFromPlayer(player.getName(), questNameId);
                        player.sendMessage("Du hast die Quest abgebrochen.");
                    }
                });
            } else {
                this.openMain(player);
            }
        }
        return true;
    }

    private void openMain(final Player player) {
        final Set<FullQuestPlayer> quests = this.getPlugin().getQuestAPI().getActivePlayerQuests(player.getName());
        final StringBuilder builder = new StringBuilder();

        if (!quests.isEmpty()) {
            quests.forEach(e -> {
                builder.append("§8» §1Quest: §7").append(e.getQuest().getDisplayName()).append("§r\n").append("§8» §1Aufgabe: §7").append(e.getQuest().getDescription()).append("§r\n")
                        .append("§8» §1Fortschritt: §f").append(e.getQuestPlayerData().getCurrent()).append("/").append(e.getQuest().getRequired()).append("\n")
                        .append("§8» §1Quest läuft ab in: §7").append(this.getPlugin().getRemainingTimeFuture(e.getQuestPlayerData().getExpire())).append("\n§8----------------------------\n");
            });
        } else builder.append("§7Du hast aktuell keine laufenden Quests. Spreche Leute am CityBuild-Spawn an, um nach einer zu fragen.");

        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Deine aktuellen Quests", "§8» §6Deine aktuellen Quests:\n\n" + builder);
        if (player.isOp()) {
            form.addButton(new ElementButton("§8» §9Quests bearbeiten"), this::openQuestAdminEdit);
            form.addButton(new ElementButton("§8» §9Quests erstellen"), this::openQuestAdminCreate);
        }
        form.build().send(player);
    }

    private void openQuestAdminEdit(final Player player) {
        final CustomForm editForm = new CustomForm.Builder("§7» §8Quest bearbeiten")
                .addElement(new ElementInput("§8» §fBitte gebe eine QuestID an, um eine Quest zu bearbeiten.", "Quest"))
                .onSubmit((b, n) -> {
                    final String nameId = n.getInputResponse(0);

                    if (nameId.isEmpty()) {
                        player.sendMessage("Fehler beim Aufrufen: Diese QuestID existiert nicht.");
                        return;
                    }

                    this.getPlugin().getQuestAPI().getQuest(nameId, quest -> {
                        if (quest == null) {
                            player.sendMessage("Fehler beim Aufrufen: Diese QuestID existiert nicht.");
                            return;
                        }
                        final CustomForm form = new CustomForm.Builder("§7» §8Quest bearbeiten")
                                .addElement(new ElementInput("§8» §fAnzeigename der Quest", "Anzeigename", quest.getDisplayName()))
                                .addElement(new ElementInput("§8» §fBeschreibe, was in dieser Quest zu tun ist.", "Beschreibung", quest.getDescription()))
                                .addElement(new ElementLabel("§8» §fBitte gebe die Aufgabe an, die in der Quest gemacht werden soll."))
                                .addElement(new ElementInput("§7- bring#§ctoggleItemTrue\n§7- collect#§ctoggleItemTrue\n§7- place#§ctoggleItemTrue\n§7- explore (Gesetzte Positionen)\n§7- craft#§ctoggleItemTrue\n§7- execute#<command>\n", "Data", quest.getData()))
                                .addElement(new ElementToggle("§8» §fDas Item in deiner Hand als Quest-Data setzen.", false))
                                .addElement(new ElementInput("§8» §fBitte gebe an, wie hoch der benötigte Wert liegen soll, bis die Quest erledigt ist. (Z. B.: Baue §c15 §fBlöcke ab.)", "15", String.valueOf(quest.getRequired())))
                                .addElement(new ElementInput("§8» §fBitte gebe an, wie lange ein Spieler für diese Quest Zeit hat. (In Stunden)", "3", String.valueOf(quest.getExpire() / 1000 / 60 / 60)))
                                .addElement(new ElementLabel("§8» §fBitte gebe die Belohnungen an, die in der Quest beinhaltet sind."))
                                .addElement(new ElementInput("Allgemeines Trennzeichen: -#-\n\n§7- xp#<amount>\n§7- money#<amount>\n§7- item#<itemSlot>\n§7- gutschein#<gutscheinData>\n§7- permission#<key>#<description>\n§7- crate#<type>#<amount>\n", "xp#200", quest.getRewardData()))
                                .addElement(new ElementToggle("§8» §fQuest-Reward-Items aktualisieren.", false))
                                .addElement(new ElementInput("§8» §fSoll diese Quest mit einem QuestNPC verlinkt werden? Wenn nicht, gebe 'null' an.", "Lola", quest.getLink()))
                                .onSubmit((g, h) -> {
                                    try {
                                        final String displayName = h.getInputResponse(0);
                                        final String description = h.getInputResponse(1);
                                        String data = h.getInputResponse(3);
                                        final boolean dataSetItem = h.getToggleResponse(4);
                                        final int required = Integer.parseInt(h.getInputResponse(5));
                                        long expire = Long.parseLong(h.getInputResponse(6));
                                        final String rawRewardData = h.getInputResponse(8);
                                        final boolean updateItems = h.getToggleResponse(9);
                                        String link = h.getInputResponse(10);

                                        String rewardData = rawRewardData;
                                        if (updateItems) {
                                            final StringBuilder rewardDataString = new StringBuilder();
                                            for (final String s : rawRewardData.split("-#-")) {
                                                final String[] f = s.split("#");
                                                if (f[0].equals("item")) {
                                                    final Item item = player.getInventory().slots.get(Integer.parseInt(f[1]));
                                                    if (item.getId() != 0) {
                                                        rewardDataString.append("item#").append(SyncAPI.ItemAPI.pureItemToStringWithCount(item)).append("-#-");
                                                    }
                                                } else rewardDataString.append(s).append("-#-");
                                            }
                                            rewardData = rewardDataString.substring(0, rewardDataString.length() - 3);
                                        }


                                        if (displayName.isEmpty() || description.isEmpty() || data.isEmpty() || rawRewardData.isEmpty()) {
                                            player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
                                            return;
                                        }

                                        if (link.isEmpty()) link = "null";

                                        if (dataSetItem) {
                                            final Item item = player.getInventory().getItemInHand();
                                            if (item.getId() == 0) {
                                                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                                                return;
                                            }
                                            data = data + "#" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                                        }

                                        if (data.startsWith("explore")) {
                                            if (!this.pos1.containsKey(player.getName()) || !this.pos2.containsKey(player.getName())) throw new NullPointerException("No locations set");
                                            final Location pos1 = this.pos1.get(player.getName());
                                            final Location pos2 = this.pos2.get(player.getName());
                                            data = "explore#" + pos1.getX() + ">" + pos1.getY() + ">" + pos1.getZ() + ">" + pos1.getLevelName() + "#" + pos2.getX() + ">" + pos2.getY() + ">" + pos2.getZ() + ">" + pos2.getLevelName();
                                        }

                                        final String finalData = data;
                                        final String finalLink = link;
                                        final long finalExpire = expire * 60 * 60 * 1000;
                                        this.getPlugin().getQuestAPI().updateQuest(nameId, displayName, description, finalData, required, finalExpire, rewardData, finalLink);
                                        player.sendMessage("Die Quest wurde soeben aktualisiert! [" + nameId + "]");
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
                                    }
                                })
                                .build();
                        form.send(player);
                    });
                })
                .build();
        editForm.send(player);
    }

    private void openQuestAdminCreate(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Neue Quest erstellen")
                .addElement(new ElementInput("§8» §fBitte gebe eine QuestID an, die einmalig ist.", "QuestID"))
                .addElement(new ElementInput("§8» §fBitte erstelle einen Anzeigenamen der Quest.", "Anzeigename"))
                .addElement(new ElementInput("§8» §fBeschreibe, was in dieser Quest zu tun ist.", "Beschreibung"))
                .addElement(new ElementLabel("§8» §fBitte gebe die Aufgabe an, die in der Quest gemacht werden soll."))
                .addElement(new ElementInput("§7- bring#§ctoggleItemTrue\n§7- collect#§ctoggleItemTrue\n§7- place#§ctoggleItemTrue\n§7- explore (Gesetzte Positionen)\n§7- craft#§ctoggleItemTrue\n§7- execute#<command>\n", "Data"))
                .addElement(new ElementToggle("§8» §fDas Item in deiner Hand als Quest-Data setzen.", false))
                .addElement(new ElementInput("§8» §fBitte gebe an, wie hoch der benötigte Wert liegen soll, bis die Quest erledigt ist. (Z. B.: Baue §c15 §fBlöcke ab.)", "15"))
                .addElement(new ElementInput("§8» §fBitte gebe an, wie lange ein Spieler für diese Quest Zeit hat. (In Stunden)", "3"))
                .addElement(new ElementLabel("§8» §fBitte gebe die Belohnungen an, die in der Quest beinhaltet sind."))
                .addElement(new ElementInput("Allgemeines Trennzeichen: -#-\n\n§7- xp#<amount>\n§7- money#<amount>\n§7- item#<itemSlot>\n§7- gutschein#<gutscheinData>\n§7- permission#<key>#<description>\n§7- crate#<type>#<amount>\n", "xp#200"))
                .addElement(new ElementInput("§8» §fSoll diese Quest mit einem QuestNPC verlinkt werden? Wenn nicht, gebe 'null' an.", "Lola"))
                .onSubmit((g, h) -> {
                    try {
                        final String nameId = h.getInputResponse(0);
                        final String displayName = h.getInputResponse(1);
                        final String description = h.getInputResponse(2);
                        String data = h.getInputResponse(4);
                        final boolean dataSetItem = h.getToggleResponse(5);
                        final int required = Integer.parseInt(h.getInputResponse(6));
                        final long expire = Long.parseLong(h.getInputResponse(7));
                        final String rawRewardData = h.getInputResponse(9);
                        String link = h.getInputResponse(10);

                        final StringBuilder rewardDataString = new StringBuilder();
                        for (final String s : rawRewardData.split("-#-")) {
                            final String[] f = s.split("#");
                            if (f[0].equals("item")) {
                                final Item item = player.getInventory().slots.get(Integer.parseInt(f[1]));
                                if (item.getId() != 0) {
                                    rewardDataString.append("item#").append(SyncAPI.ItemAPI.pureItemToStringWithCount(item)).append("-#-");
                                }
                            } else rewardDataString.append(s).append("-#-");
                        }
                        final String rewardData = rewardDataString.substring(0, rewardDataString.length() - 3);

                        if (nameId.isEmpty() || displayName.isEmpty() || description.isEmpty() || data.isEmpty() || rawRewardData.isEmpty()) {
                            player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
                            return;
                        }

                        if (link.isEmpty()) link = "null";

                        if (dataSetItem) {
                            final Item item = player.getInventory().getItemInHand();
                            if (item.getId() == 0) {
                                player.sendMessage("Deine Angaben sind fehlerhaft: Bitte halte ein Item in deiner Hand.");
                                return;
                            }
                            data = data + "#" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                        }

                        if (data.startsWith("explore")) {
                            if (!this.pos1.containsKey(player.getName()) || !this.pos2.containsKey(player.getName())) throw new NullPointerException("No locations set");
                            final Location pos1 = this.pos1.get(player.getName());
                            final Location pos2 = this.pos2.get(player.getName());
                            data = "explore#" + pos1.getX() + ">" + pos1.getY() + ">" + pos1.getZ() + ">" + pos1.getLevelName() + "#" + pos2.getX() + ">" + pos2.getY() + ">" + pos2.getZ() + ">" + pos2.getLevelName();
                        }

                        final String finalData = data;
                        final String finalLink = link;
                        this.getPlugin().getQuestAPI().getQuest(nameId, quest -> {
                            if (quest == null) {
                                this.getPlugin().getQuestAPI().createQuest(nameId, displayName, description, finalData, required, (expire * 60 * 60 * 1000), rewardData, finalLink);
                                player.sendMessage("Die Quest wurde soeben erstellt! [" + nameId + "]");
                            } else player.sendMessage("Fehler beim Erstellen: Diese QuestID wird bereits verwendet.");
                        });
                    } catch (final Exception e) {
                        player.sendMessage("Fehler beim Erstellen: Bitte überprüfe deine Angaben.");
                    }
                })
                .build();
        form.send(player);
    }
}
