package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.CraftItemEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.permission.PermissionAttachment;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.economy.components.economy.event.MoneyChangeEvent;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.ScoreboardAPI;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.event.QuestCompleteEvent;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.scoreboard.network.DisplayEntry;
import net.eltown.servercore.components.scoreboard.network.DisplaySlot;
import net.eltown.servercore.components.scoreboard.network.ScoreboardDisplay;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.*;

@RequiredArgsConstructor
public class EventListener implements Listener {

    private final ServerCore instance;

    public static final HashMap<String, PermissionAttachment> attachments = new HashMap<>();

    public static Set<String> needsIntroduction = new HashSet<>();
    public static Set<String> inIntroduction = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerLocallyInitializedEvent event) {
        final Player player = event.getPlayer();
        player.setCheckMovement(false);

        /*
         * Sync
         */
        this.instance.getSyncAPI().loadPlayer(player, loaded -> {
            if (loaded) {
                /*
                 * Teleportation
                 */
                this.instance.getTinyRabbit().sendAndReceive((data) -> {
                    switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                        case CALLBACK_CACHED_DATA:
                            final String[] d = data.getData();
                            if (d[1].startsWith("WARP_NULL==&")) {
                                final String[] p = d[1].split("==&");
                                player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                                player.sendMessage(Language.get("warp.teleported", p[1]));
                            } else if (d[1].startsWith("TPA_NULL==&")) {
                                final String[] p = d[1].split("==&");
                                player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                                player.sendMessage(Language.get("tpa.teleported", p[1]));
                            } else if (d[1].startsWith("TP_NULL==&")) {
                                final String[] p = d[1].split("==&");
                                final Player target = this.instance.getServer().getPlayer(p[1]);
                                if (target != null) player.teleport(target.getLocation());
                            } else {
                                player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                                player.sendMessage(Language.get("home.teleported", d[1]));
                            }
                            break;
                        default:
                            break;
                    }
                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_CACHED_DATA.name(), player.getName());

                /*
                 * Groups
                 */
                this.instance.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_FULL_GROUP_PLAYER:
                            final String prefix = delivery.getData()[3];
                            final String[] permissions = delivery.getData()[4].split("#");
                            final String[] aPermissions = delivery.getData()[6].split("#");

                            attachments.remove(player.getName());
                            attachments.put(player.getName(), player.addAttachment(this.instance));
                            final PermissionAttachment attachment = attachments.get(player.getName());

                            for (final String p : permissions) {
                                attachment.setPermission(p, true);
                            }

                            for (final String p : aPermissions) {
                                if (!attachment.getPermissions().containsKey(p)) attachment.setPermission(p, true);
                            }

                            player.setNameTag(prefix.replace("%p", player.getName()));
                            break;
                    }
                }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP_PLAYER.name(), player.getName());

                /*
                 * Level
                 */
                this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                    switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_LEVEL:
                            this.instance.getLevelAPI().cachedData.put(player.getName(), new Level(
                                    delivery.getData()[1],
                                    Integer.parseInt(delivery.getData()[2]),
                                    Double.parseDouble(delivery.getData()[3])
                            ));
                            player.setScoreTag("§gLevel §l" + Integer.parseInt(delivery.getData()[2]));
                            break;
                    }
                }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_GET_LEVEL.name(), player.getName());

                /*
                 * Scoreboard
                 */
                Economy.getAPI().getMoney(player, money -> {
                    final ScoreboardDisplay scoreboard = ScoreboardAPI.createScoreboard().addDisplay(DisplaySlot.SIDEBAR, "eltown", "§2§lEltown.net");
                    scoreboard.addLine("§1", 0);
                    scoreboard.addLine(" §8» §0Bargeld", 1);
                    final DisplayEntry economyEntry = scoreboard.addLine("   §f$" + Economy.getAPI().getMoneyFormat().format(money), 2);
                    scoreboard.addLine("§1§1", 3);
                    scoreboard.addLine(" §8» §0Level", 4);
                    final DisplayEntry levelEntry = scoreboard.addLine("   §f" + this.instance.getLevelAPI().getLevel(player.getName()).getLevel() + " §8[" + this.instance.getLevelAPI().getLevelDisplay(player) + "§8]  ", 5);
                    scoreboard.addLine("§1§1§1", 6);

                    ScoreboardAPI.setScoreboard(player, scoreboard.getScoreboard());
                    ScoreboardAPI.cachedData.put(player.getName(), scoreboard);
                    ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/economy", economyEntry);
                    ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/level", levelEntry);
                });

                /*
                 * Friends
                 */
                this.instance.getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIEND_DATA.name(), player.getName());

                /*
                 * Quests
                 */
                this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                    final String[] d = delivery.getData();
                    try {
                        switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_PLAYER_DATA:
                                final List<QuestPlayer.QuestPlayerData> questPlayerData = new ArrayList<>();

                                if (!d[1].equals("null")) {
                                    for (String s : d[1].split("-#-")) {
                                        final String[] sSplit = s.split("-:-");
                                        questPlayerData.add(new QuestPlayer.QuestPlayerData(sSplit[0], Long.parseLong(sSplit[1]), Integer.parseInt(sSplit[2]), Integer.parseInt(sSplit[3])));
                                    }
                                }
                                this.instance.getQuestAPI().cachedQuestPlayer.put(player.getName(), new QuestPlayer(player.getName(), questPlayerData));
                                this.instance.getQuestAPI().checkIfQuestIsExpired(player.getName());
                                break;
                            case CALLBACK_NULL:
                                this.instance.getQuestAPI().cachedQuestPlayer.put(player.getName(), new QuestPlayer(player.getName(), new ArrayList<>()));
                                break;
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_PLAYER_DATA.name(), player.getName());

                if (needsIntroduction.contains(player.getName()) || inIntroduction.contains(player.getName())) {
                    Economy.getAPI().setMoney(player, 0);
                    player.getInventory().clearAll();

                    player.teleport(new Location(53, 106, 54, this.instance.getServer().getLevelByName("plots")));
                    needsIntroduction.add(player.getName());
                    player.sendTitle("§0§lWILLKOMMEN auf", "§2Eltown.net", 20, 60, 30);
                    player.sendMessage("§c§lFühre den Befehl §f§l/start §c§laus, um fortzufahren.");
                    this.instance.playSound(player, Sound.RANDOM_TOAST);
                    return;
                }

                if (!player.hasPlayedBefore() && this.instance.getServerName().equals("server-1")) {
                    player.teleport(new Location(53, 106, 54, this.instance.getServer().getLevelByName("plots")));
                    needsIntroduction.add(player.getName());
                    player.sendTitle("§0§lWILLKOMMEN auf", "§2Eltown.net", 20, 60, 30);
                    player.sendMessage("§c§lFühre den Befehl §f§l/start §c§laus, um fortzufahren.");
                    this.instance.playSound(player, Sound.RANDOM_TOAST);
                }
            }
        });
    }

    @EventHandler
    public void on(final MoneyChangeEvent event) {
        final ScoreboardDisplay scoreboardDisplay = ScoreboardAPI.cachedData.get(event.getPlayer().getName());
        final DisplayEntry displayEntry = ScoreboardAPI.cachedDisplayEntries.get(event.getPlayer().getName() + "/economy");
        scoreboardDisplay.removeEntry(displayEntry);

        final DisplayEntry economyEntry = scoreboardDisplay.addLine("   §f$" + Economy.getAPI().getMoneyFormat().format(event.getMoney()), 2);
        ScoreboardAPI.cachedDisplayEntries.put(event.getPlayer().getName() + "/economy", economyEntry);
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        event.setJoinMessage("");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final QuestCompleteEvent event) {
        final Player player = event.getPlayer();
        final Quest quest = event.getQuest();
        if (quest.getNameId().equals("start1")) {
            player.sendMessage(" ");
            player.sendMessage("§8» §fEltown §8| §7Auf §0Eltown §7gibt es §05 Marktstände§7, an denen Items §0ge- und verkauft §7werden können. Die Preise der einzelnen Märkte werden durch die Anzahl der ge- oder verkauften Items beeinflusst.");
            player.sendMessage(" ");

            final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Auf §0Eltown §7gibt es §05 Marktstände§7, an denen Items §0ge- und verkauft §7werden können. Die Preise der einzelnen Märkte werden durch die Anzahl der ge- oder verkauften Items beeinflusst.")
                    .addButton(new ElementButton("§8» §2Reise fortsetzen"), e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start2");
                        this.instance.getQuestAPI().getQuest("start2", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7Auch du kannst mit Items §0handeln§7! Dafür stehen dir die §0ChestShops §7zur Verfügung. Diese sind allerdings erst ab §0Level 2 §7freigeschaltet und du benötigst ein §0Bankkonto§7. Natürlich kann man auch ohne ChestShops handeln, aber dir könnten Betrüger über den Weg laufen. Also sei auf der Hut!");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Auch du kannst mit Items §0handeln§7! Dafür stehen dir die §0ChestShops §7zur Verfügung. Diese sind allerdings erst ab §0Level 2 §7freigeschaltet und du benötigst ein §0Bankkonto§7. " +
                                    "Natürlich kann man auch ohne ChestShops handeln, aber dir könnten Betrüger über den Weg laufen. Also sei auf der Hut!\n" +
                                    "\n§fDaher lautet deine neue Aufgabe:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .onClose(e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start2");
                        this.instance.getQuestAPI().getQuest("start2", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7Auch du kannst mit Items §0handeln§7! Dafür stehen dir die §0ChestShops §7zur Verfügung. Diese sind allerdings erst ab §0Level 2 §7freigeschaltet und du benötigst ein §0Bankkonto§7. Natürlich kann man auch ohne ChestShops handeln, aber dir könnten Betrüger über den Weg laufen. Also sei auf der Hut!");
                            player.sendMessage("§8» §fEltown §8| §fDaher lautet deine neue Aufgabe:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Auch du kannst mit Items §0handeln§7! Dafür stehen dir die §0ChestShops §7zur Verfügung. Diese sind allerdings erst ab §0Level 2 §7freigeschaltet und du benötigst ein §0Bankkonto§7. " +
                                    "Natürlich kann man auch ohne ChestShops handeln, aber dir könnten Betrüger über den Weg laufen. Also sei auf der Hut!\n" +
                                    "\n§fDaher lautet deine neue Aufgabe:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .build();
            form.send(player);
        } else if (quest.getNameId().equals("start2")) {
            player.sendMessage(" ");
            player.sendMessage("§8» §fEltown §8| §7In der Bank kannst du ein §0Bankkonto §7erstellen, um beispielsweise die ChestShops nutzen zu können oder um darin einkaufen zu können. Bankkonten bieten sich aber auch gut an, um mit §0Freunden §7oder §0Gemeinschaften §7Geld zu teilen. Geld kann mit der jeweiligen §0Bankkarte §7an den §0Geldautomaten §7ein- oder ausgezahlt werden. In der ersten Etage der Bank kann man §0Cryptowährung §7kaufen.");
            player.sendMessage(" ");

            final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7In der Bank kannst du ein §0Bankkonto §7erstellen, um beispielsweise die ChestShops nutzen zu können oder um darin einkaufen zu können. Bankkonten bieten sich aber auch gut an, um mit §0Freunden §7oder §0Gemeinschaften §7Geld zu teilen. Geld kann mit der jeweiligen §0Bankkarte §7an den §0Geldautomaten §7ein- oder ausgezahlt werden. In der ersten Etage der Bank kann man §0Cryptowährung §7kaufen.")
                    .addButton(new ElementButton("§8» §2Reise fortsetzen"), e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start3");
                        this.instance.getQuestAPI().getQuest("start3", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §0Kryptowährung §7wird nach und nach eine größere Rolle spielen. Über kommende §0Updates §7wirst du auf unserem §0Discord-Server §7informiert. Du kannst diesen unter §9www.eltown.net/discord §7beitreten.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §0Kryptowährung §7wird nach und nach eine größere Rolle spielen. Über kommende §0Updates §7wirst du auf unserem §0Discord-Server §7informiert. Du kannst diesen unter §9www.eltown.net/discord §7beitreten.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .onClose(e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start3");
                        this.instance.getQuestAPI().getQuest("start3", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §0Kryptowährung §7wird nach und nach eine größere Rolle spielen. Über kommende §0Updates §7wirst du auf unserem §0Discord-Server §7informiert. Du kannst diesen unter §9www.eltown.net/discord §7beitreten.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §0Kryptowährung §7wird nach und nach eine größere Rolle spielen. Über kommende §0Updates §7wirst du auf unserem §0Discord-Server §7informiert. Du kannst diesen unter §9www.eltown.net/discord §7beitreten.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .build();
            form.send(player);
        } else if (quest.getNameId().equals("start3")) {
            player.sendMessage(" ");
            player.sendMessage("§8» §fEltown §8| §7Im Dorf findest du sehr nette Leute. Du kannst diese zu jeder Zeit ansprechen. Auf unserem §0Discord-Server §7kannst du bei §0Problemen oder Fragen §7auch ganz einfach ein Ticket öffnen, damit sich ein Teammitglied um dich kümmert. Falls du keinen Discord-Account hast, dann kannst du auch hier im Spiel ein Ticket öffnen mit §0/ticket §7oder mit §0/support§7.");
            player.sendMessage(" ");

            final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Im Dorf findest du sehr nette Leute. Du kannst diese zu jeder Zeit ansprechen.\n§7Auf unserem §0Discord-Server §7kannst du bei §0Problemen oder Fragen §7auch ganz einfach ein Ticket öffnen, damit sich ein Teammitglied um dich kümmert. Falls du keinen Discord-Account hast, dann kannst du auch hier im Spiel ein Ticket öffnen mit §0/ticket §7oder mit §0/support§7.")
                    .addButton(new ElementButton("§8» §2Reise fortsetzen"), e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start4");
                        this.instance.getQuestAPI().getQuest("start4", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7In der alten §0Ruine §7können bald merkwürde Dinge gehandelt werden... Aber die §0Polizei §7hat bereits ein Auge auf diesen Platz geworfen. Die Ratsmitglieder würden gerne den Wald abholzen, um weitere Wohnhäuser zu bauen, aber der Inhaber der Ruine ist strikt dagegen.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7In der alten §0Ruine §7können bald merkwürde Dinge gehandelt werden... Aber die §0Polizei §7hat bereits ein Auge auf diesen Platz geworfen. Die Ratsmitglieder würden gerne den Wald abholzen, um weitere Wohnhäuser zu bauen, aber der Inhaber der Ruine ist strikt dagegen.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .onClose(e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start4");
                        this.instance.getQuestAPI().getQuest("start4", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7In der alten §0Ruine §7können bald merkwürde Dinge gehandelt werden... Aber die §0Polizei §7hat bereits ein Auge auf diesen Platz geworfen. Die Ratsmitglieder würden gerne den Wald abholzen, um weitere Wohnhäuser zu bauen, aber der Inhaber der Ruine ist strikt dagegen.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7In der alten §0Ruine §7können bald merkwürde Dinge gehandelt werden... Aber die §0Polizei §7hat bereits ein Auge auf diesen Platz geworfen. Die Ratsmitglieder würden gerne den Wald abholzen, um weitere Wohnhäuser zu bauen, aber der Inhaber der Ruine ist strikt dagegen.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .build();
            form.send(player);
        } else if (quest.getNameId().equals("start4")) {
            player.sendMessage(" ");
            player.sendMessage("§8» §fEltown §8| §7Es gibt noch eine weitere §0Person §7in diesem Wald, die dort illegal wohnt. Diese Person heißt §0Lisa §7und freut sich über jeden §0Besuch§7. Am §0Waldrand §7kannst du §0Lola §7finden, die dir täglich eine ihrer §0Geschenke §7schenkt. Sie anzusprechen lohnt sich immer!");
            player.sendMessage(" ");

            final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Es gibt noch eine weitere §0Person §7in diesem Wald, die dort illegal wohnt. Diese Person heißt §0Lisa §7und freut sich über jeden §0Besuch§7. Am §0Waldrand §7kannst du §0Lola §7finden, die dir täglich eine ihrer §0Geschenke §7schenkt. Sie anzusprechen lohnt sich immer!")
                    .addButton(new ElementButton("§8» §2Reise fortsetzen"), e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start5");
                        this.instance.getQuestAPI().getQuest("start5", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7Und zum Schluss das wohl wichtigste Gebäude: Das §0Rathaus§7. Im Rathaus kannst du §0Termine mit Mitarbeitern §7vereinbaren, um zum Beispiel eine neue §0ChestShop-Lizenz §7zu erwerben. Demnächst wird das Rathaus noch weiter in den Mittelpunkt rücken.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Und zum Schluss das wohl wichtigste Gebäude: Das §0Rathaus§7. Im Rathaus kannst du §0Termine mit Mitarbeitern §7vereinbaren, um zum Beispiel eine neue §0ChestShop-Lizenz §7zu erwerben. Demnächst wird das Rathaus noch weiter in den Mittelpunkt rücken.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .onClose(e -> {
                        this.instance.getQuestAPI().setQuestOnPlayer(player.getName(), "start5");
                        this.instance.getQuestAPI().getQuest("start5", newQuest -> {
                            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                            player.sendMessage(" ");
                            player.sendMessage("§8» §fEltown §8| §7Und zum Schluss das wohl wichtigste Gebäude: Das §0Rathaus§7. Im Rathaus kannst du §0Termine mit Mitarbeitern §7vereinbaren, um zum Beispiel eine neue §0ChestShop-Lizenz §7zu erwerben. Demnächst wird das Rathaus noch weiter in den Mittelpunkt rücken.");
                            player.sendMessage("§8» §fEltown §8| §fDeine neue Aufgabe lautet:");
                            player.sendMessage("§8» §fEltown §8| " + newQuest.getDescription());
                            player.sendMessage(" ");

                            final SimpleForm questForm = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Und zum Schluss das wohl wichtigste Gebäude: Das §0Rathaus§7. Im Rathaus kannst du §0Termine mit Mitarbeitern §7vereinbaren, um zum Beispiel eine neue §0ChestShop-Lizenz §7zu erwerben. Demnächst wird das Rathaus noch weiter in den Mittelpunkt rücken.\n" +
                                    "\n§fDeine neue Aufgabe lautet:\n§r" + newQuest.getDescription() + "\n\n")
                                    .addButton(new ElementButton("§8» §aOkay"))
                                    .build();
                            questForm.send(player);
                        });
                    })
                    .build();
            form.send(player);
        } else if (quest.getNameId().equals("start5")) {
            player.sendMessage(" ");
            player.sendMessage("§8» §fEltown §8| §7Nun hast du den §0CityBuild-Spawn §7kennengelernt und weißt genau, wo was zu finden ist. Mit §0/warp §7kannst du zwischen Welten bzw. Servern hin und her springen und mit §0/p §7rufst du die §0Plotbefehle §7auf.");
            player.sendMessage(" ");

            new ChainExecution.Builder()
                    .append(4, () -> {
                        this.instance.getQuestAPI().removeQuestFromPlayer(player.getName(), "start1");
                    })
                    .append(4, () -> {
                        this.instance.getQuestAPI().removeQuestFromPlayer(player.getName(), "start2");
                    })
                    .append(4, () -> {
                        this.instance.getQuestAPI().removeQuestFromPlayer(player.getName(), "start3");
                    })
                    .append(4, () -> {
                        this.instance.getQuestAPI().removeQuestFromPlayer(player.getName(), "start4");
                    })
                    .append(4, () -> {
                        this.instance.getQuestAPI().removeQuestFromPlayer(player.getName(), "start5");
                    })
                    .build().start();

            final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Nun hast du den §0CityBuild-Spawn §7kennengelernt und weißt genau, wo was zu finden ist. Mit §0/warp §7kannst du zwischen Welten bzw. Servern hin und her springen und mit §0/p §7rufst du die §0Plotbefehle §7auf.")
                    .addButton(new ElementButton("§8» §2§lSpiel beginnen"), e -> {
                        this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                        inIntroduction.remove(player.getName());
                        player.sendMessage(" ");
                        player.sendMessage("§8» §fEltown §8| §7Bei §fLola §7am CityBuild-Spawn kannst du dir deine §fStartitems §7in Form eines §fGutscheins §7abholen.");
                        player.sendMessage("§8» §fEltown §8| §2§lVIEL SPAß!");
                        player.sendMessage(" ");
                    })
                    .onClose(e -> {
                        this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                        inIntroduction.remove(player.getName());
                        player.sendMessage(" ");
                        player.sendMessage("§8» §fEltown §8| §7Bei §fLola §7am CityBuild-Spawn kannst du dir deine §fStartitems §7in Form eines §fGutscheins §7abholen.");
                        player.sendMessage("§8» §fEltown §8| §2§lVIEL SPAß!");
                        player.sendMessage(" ");
                    })
                    .build();
            form.send(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        event.setQuitMessage("");

        if (this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) {
            this.instance.getSyncAPI().savePlayer(event.getPlayer());

            ScoreboardAPI.cachedDisplayEntries.remove(event.getPlayer().getName() + "/economy");
            ScoreboardAPI.cachedDisplayEntries.remove(event.getPlayer().getName() + "/level");

            final Level level = this.instance.getLevelAPI().getLevel(player.getName());
            this.instance.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_TO_DATABASE.name(),
                    player.getName(), String.valueOf(level.getLevel()), String.valueOf(level.getExperience()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerMoveEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);

        if (needsIntroduction.contains(event.getPlayer().getName())) {
            event.getPlayer().sendActionBar("§c§lFühre den Befehl §f§l/start §c§laus, um fortzufahren.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerDropItemEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerCommandPreprocessEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);

        if (needsIntroduction.contains(event.getPlayer().getName())) {
            if (!event.getMessage().equalsIgnoreCase("/start")) {
                event.getPlayer().sendMessage("§c§lFühre den Befehl §f§l/start §c§laus, um fortzufahren.");
                event.setCancelled(true);
            }
        } else if (inIntroduction.contains(event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final CraftItemEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEntityEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

}
