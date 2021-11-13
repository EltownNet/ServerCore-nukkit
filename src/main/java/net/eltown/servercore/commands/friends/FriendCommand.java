package net.eltown.servercore.commands.friends;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import cn.nukkit.level.Sound;
import net.eltown.economy.Economy;
import net.eltown.quadplots.QuadPlots;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FriendCommand extends PluginCommand<ServerCore> {

    public FriendCommand(final ServerCore owner) {
        super("friend", owner);
        this.setDescription("Verwalte deine Freundesliste");
        this.setAliases(Arrays.asList("f", "freunde").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openMainMenu(player);
        }
        return true;
    }

    private void openMainMenu(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (FriendCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FRIEND_DATA:
                    List<String> friends = new ArrayList<>();
                    List<String> requests = new ArrayList<>();
                    if (!delivery.getData()[1].equals("null")) friends = Arrays.asList(delivery.getData()[1].split(":"));
                    if (!delivery.getData()[2].equals("null")) requests = Arrays.asList(delivery.getData()[2].split(":"));

                    final List<String> finalFriends = friends;
                    final List<String> finalRequests = requests;
                    final SimpleForm form = new SimpleForm.Builder("§7» §8Freunde", "")
                            .addButton(new ElementButton("§8» §9Deine Freunde §8[§f" + friends.size() + "§8]", new ElementButtonImageData("url", "")), e -> this.openFriends(player, finalFriends))
                            .addButton(new ElementButton("§8» §9Offene Anfragen §8[§f" + requests.size() + "§8]", new ElementButtonImageData("url", "")), e -> this.openRequests(player, finalRequests))
                            .addButton(new ElementButton("§8» §9Freund hinzufügen", new ElementButtonImageData("url", "")), e -> this.openAddFriend(player, finalFriends, finalRequests))
                            .addButton(new ElementButton("§8» §9Einstellungen", new ElementButtonImageData("url", "")), this::openSettings)
                            .build();
                    form.send(player);
                    break;
            }
        }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_FRIEND_DATA.name(), player.getName());
    }

    private void openAddFriend(final Player player, final List<String> friends, final List<String> requests) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GET_ONLINE_PLAYERS:
                    final List<String> players = new ArrayList<>(Collections.singletonList("Spieler wählen!"));
                    players.addAll(Arrays.asList(delivery.getData()[1].split("#")));
                    players.remove(player.getName());
                    final CustomForm form = new CustomForm.Builder("§7» §8Freund hinzufügen")
                            .addElement(new ElementDropdown("§8» §fWähle einen Spieler, der grade online ist, aus, um diesem eine Freundschaftsanfrage zu senden.", players))
                            .addElement(new ElementInput("§8» §fAlternativ kannst du auch einen Spieler angeben, der aktuell nicht online ist.", "HanzFranz"))
                            .onSubmit((g, h) -> {
                                final String drop = h.getDropdownResponse(0).getElementContent();
                                final String input = h.getInputResponse(1);

                                String target = "";
                                if (!drop.equals("Spieler wählen!")) {
                                    target = drop;
                                } else {
                                    if (!input.isEmpty()) {
                                        target = input;
                                    } else {
                                        player.sendMessage(Language.get("friend.invalid.input"));
                                        this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                        return;
                                    }
                                }

                                if (target.equalsIgnoreCase(player.getName())) {
                                    player.sendMessage(Language.get("friend.invalid.input"));
                                    this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                    return;
                                }

                                if (friends.contains(target)) {
                                    player.sendMessage(Language.get("friend.already.friends", target));
                                    this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                    return;
                                }

                                if (requests.contains(target)) {
                                    this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIENDSHIP.name(), player.getName(), target);
                                    this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), target);
                                    this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), target, player.getName());
                                    this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), target, Language.get("friend.friendship.created", player.getName()));
                                    player.sendMessage(Language.get("friend.friendship.created", target));
                                    this.getPlugin().playSound(player, Sound.NOTE_HARP);
                                    return;
                                }

                                final String finalTarget = target;
                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery1 -> {
                                    switch (FriendCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_NULL:
                                            player.sendMessage(Language.get("friend.invalid.player"));
                                            this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                            break;
                                        case CALLBACK_FRIEND_DATA:
                                            this.getPlugin().getSettingsAPI().getEntry(finalTarget, "friend/requests", "true", value -> {
                                                if (value.equals("true")) {
                                                    this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIEND_REQUEST.name(), player.getName(), finalTarget);
                                                    player.sendMessage(Language.get("friend.request.created", finalTarget));
                                                    this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), finalTarget, Language.get("friend.request.received", player.getName()));
                                                    this.getPlugin().playSound(player, Sound.NOTE_HARP);
                                                } else {
                                                    player.sendMessage(Language.get("friend.settings.no.requests", finalTarget));
                                                    this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                                }
                                            });
                                            break;
                                    }
                                }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_FRIEND_DATA.name(), target);
                            })
                            .build();
                    form.send(player);
                    break;
            }
        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
    }

    public void openFriends(final Player player, final List<String> friends) {
        if (friends.size() == 0) {
            player.sendMessage(Language.get("friend.no.friends"));
            this.getPlugin().playSound(player, Sound.NOTE_BASS);
            return;
        }

        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Freundesliste", "§8» §fHier sind deine Freunde aufgelistet. Klicke einen an, um diesen zu verwalten oder zu interagieren.");

        friends.forEach(e -> {
            this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                switch (CoreCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_NULL:
                        form.addButton(new ElementButton("§8» §f" + e + " §8[§cOffline§8]\n§9Verwalten"), g -> this.openManageFriend(player, e));
                        break;
                    case CALLBACK_ONLINE:
                        form.addButton(new ElementButton("§8» §f" + e + " §8[§aOnline§8]\n§9Verwalten"), g -> this.openManageFriend(player, e));
                        break;
                }
            }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), e);
        });

        form.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "")), this::openMainMenu);

        form.build().send(player);
    }

    public void openFriends(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (FriendCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_FRIEND_DATA:
                    List<String> friends = new ArrayList<>();
                    if (!delivery.getData()[1].equals("null")) friends = Arrays.asList(delivery.getData()[1].split(":"));

                    if (friends.size() == 0) {
                        player.sendMessage(Language.get("friend.no.friends"));
                        this.getPlugin().playSound(player, Sound.NOTE_BASS);
                        return;
                    }

                    final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Freundesliste", "§8» §fHier sind deine Freunde aufgelistet. Klicke einen an, um diesen zu verwalten oder zu interagieren.");

                    friends.forEach(e -> {
                        this.getPlugin().getTinyRabbit().sendAndReceive(delivery1 -> {
                            switch (CoreCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                case CALLBACK_NULL:
                                    form.addButton(new ElementButton("§8» §f" + e + " §8[§cOffline§8]\n§9Verwalten"), g -> this.openManageFriend(player, e));
                                    break;
                                case CALLBACK_ONLINE:
                                    form.addButton(new ElementButton("§8» §f" + e + " §8[§aOnline§8]\n§9Verwalten"), g -> this.openManageFriend(player, e));
                                    break;
                            }
                        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_IS_PLAYER_ONLINE.name(), e);
                    });

                    form.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "")), this::openMainMenu);

                    form.build().send(player);
                    break;
            }
        }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_FRIEND_DATA.name(), player.getName());
    }

    private void openManageFriend(final Player player, final String friend) {
        final StringBuilder content = new StringBuilder("§8» §fHier kannst du deinen Freund oder deine Freundin §9" + friend + " §fverwalten. Schau dir die Informationen an oder interagiere mit den folgenden Möglichkeiten.\n\n");

        Economy.getAPI().getMoney(friend, money -> {
            this.getPlugin().getSettingsAPI().getEntry(friend, "friend/level", "true", value -> {
                if (value.equals("true")) {
                    content.append("§8» §9Level: §f").append(this.getPlugin().getLevelAPI().getLevel(friend).getLevel()).append("\n");
                }
            });

            this.getPlugin().getSettingsAPI().getEntry(friend, "friend/money", "true", value -> {
                if (value.equals("true")) {
                    content.append("§8» §9Bargeld: §f$").append(Economy.getAPI().getMoneyFormat().format(money)).append("\n");
                }
            });

            this.getPlugin().getSettingsAPI().getEntry(friend, "friend/onlinetime", "true", value -> {
                if (value.equals("true")) {
                    this.getPlugin().getTinyRabbit().sendAndReceive(delivery2 -> {
                        if (delivery2.getKey().equalsIgnoreCase("REQUEST_PLAYTIME")) {
                            final long day = Long.parseLong(delivery2.getData()[1]);
                            final long hours = day / 1000 / 60 / 60;
                            content.append("§8» §9Spielzeit: §f").append(hours).append(hours == 1 ? " Stunde" : " Stunden").append("\n");
                        }
                    }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", friend);
                }
            });

            content.append("\n");

            final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Freund verwalten", content.toString());
            this.getPlugin().getTinyRabbit().sendAndReceive((delivery1 -> {
                switch (CoreCalls.valueOf(delivery1.getKey().toUpperCase())) {
                    case CALLBACK_GET_ONLINE_PLAYERS:
                        final List<String> players = Arrays.asList(delivery1.getData()[1].split("#"));
                        if (players.contains(friend)) {
                            form.addButton(new ElementButton("§8» §fTPA senden", new ElementButtonImageData("url", "")), e -> {

                                this.getPlugin().getTinyRabbit().sendAndReceive(delivery2 -> {
                                    switch (TeleportationCalls.valueOf(delivery2.getKey().toUpperCase())) {
                                        case CALLBACK_TPA_ALREADY_SENT:
                                            player.sendMessage(Language.get("tpa.already.sent", friend));
                                            this.getPlugin().playSound(player, Sound.NOTE_BASS);
                                            break;
                                        case CALLBACK_NULL:
                                            player.sendMessage(Language.get("tpa.sent", friend));
                                            this.getPlugin().playSound(player, Sound.NOTE_HARP);
                                            break;
                                    }
                                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_SEND_TPA.name(), player.getName(), friend);

                            });
                        }
                        break;
                }
            }), Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
            if (this.getPlugin().getServerName().equals("server-1")) {
                if (QuadPlots.getApi().getProvider().getPlots(friend).size() > 0) {
                    form.addButton(new ElementButton("§8» §fGrundstücke §8[§f" + QuadPlots.getApi().getProvider().getPlots(friend).size() + "§8]", new ElementButtonImageData("url", "")), e -> {
                        final SimpleForm.Builder plots = new SimpleForm.Builder("§7» §8Grundstücke eines Freundes", "§8» §fKlicke eines dieser Grundstücke deines Freundes oder deiner Freundin §9" + friend + " §fan, um dich zu diesem zu teleportieren.");

                        QuadPlots.getApi().getProvider().getPlots(friend).forEach(plot -> {
                            plots.addButton(new ElementButton("§8» §f" + plot.getStringId() + " §8| §f" + plot.getName() + "\n§9Teleportieren"), g -> {
                                player.teleport(plot.getPosition());
                                player.sendMessage(Language.get("friend.plot.teleported", plot.getStringId(), friend));
                                this.getPlugin().playSound(player, Sound.NOTE_HARP);
                            });
                        });
                        plots.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "")), f -> this.openManageFriend(player, friend));
                        plots.build().send(player);
                    });
                }
            }

            form.addButton(new ElementButton("§8» §cFreund entfernen", new ElementButtonImageData("url", "")), e -> {
                this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                    switch (FriendCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_ARE_FRIENDS:
                            final boolean are = Boolean.parseBoolean(delivery.getData()[1]);
                            if (are) {
                                final ModalForm modalForm = new ModalForm.Builder("§7» §8Freund entfernen", "§fMöchtest du die Freundschaft mit §9" + friend + " §fbeenden? Diese Aktion kann nicht rückgängig gemacht werden.",
                                        "§8» §aEntfernen", "§8» §cAbbrechen")
                                        .onYes(g -> {
                                            this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIENDSHIP.name(), player.getName(), friend);
                                            this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIENDSHIP.name(), friend, player.getName());
                                            this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), friend, Language.get("friend.friendship.removed", player.getName()));
                                            player.sendMessage(Language.get("friend.friendship.removed", friend));
                                            this.getPlugin().playSound(player, Sound.NOTE_HARP);
                                        })
                                        .onNo(g -> this.openManageFriend(player, friend))
                                        .build();
                                modalForm.send(player);
                            } else {
                                player.sendMessage(Language.get("friend.no.friendship", friend));
                                this.getPlugin().playSound(player, Sound.NOTE_BASS);
                            }
                            break;
                    }
                }, Queue.FRIEND_CALLBACK, FriendCalls.REQUEST_CHECK_ARE_FRIENDS.name(), player.getName(), friend);
            });

            form.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "")), e -> this.openFriends(player));

            form.build().send(player);
        });
    }

    public void openRequests(final Player player, final List<String> requests) {
        if (requests.size() == 0) {
            player.sendMessage(Language.get("friend.no.requests"));
            this.getPlugin().playSound(player, Sound.NOTE_BASS);
            return;
        }

        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Freundschaftsanfragen", "§8» §fKlicke eine Anfrage an, um diese anzunehmen oder abzulehnen.\n\n");

        requests.forEach(e -> {
            form.addButton(new ElementButton("§8» §f" + e + "\n§9Verwalten"), g -> {
                final ModalForm modalForm = new ModalForm.Builder("§7» §8Freundschaftsanfrage verwalten", "§fMöchtest du die Freundschaftsanfrage von §9" + e + " §fannehmen oder ablehnen?\n\nWenn du diese annimmst, dann stehen dir in Verbindung mit diesem Spieler alle Freunde-Funktionen zur Verfügung.",
                        "§8» §aAnnehmen", "§8» §cAblehnen")
                        .onYes(d -> {
                            this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIENDSHIP.name(), player.getName(), e);
                            this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), e);
                            this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), e, Language.get("friend.friendship.created", player.getName()));
                            player.sendMessage(Language.get("friend.friendship.created", e));
                            this.getPlugin().playSound(player, Sound.NOTE_HARP);
                        })
                        .onNo(d -> {
                            this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_REMOVE_FRIEND_REQUEST.name(), player.getName(), e);
                            this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_SEND_PLAYER_MESSAGE.name(), e, Language.get("friend.request.denied.other", player.getName()));
                            player.sendMessage(Language.get("friend.request.denied", e));
                            this.getPlugin().playSound(player, Sound.NOTE_HARP);
                        })
                        .build();
                modalForm.send(player);
            });
        });

        form.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "")), this::openMainMenu);

        form.build().send(player);
    }

    public void openSettings(final Player player) {
        final CustomForm.Builder form = new CustomForm.Builder("§7» §8Einstellungen");

        form.addElement(new ElementLabel("§8» §fHier kannst du Einstellungen bezüglich deiner Freunde treffen.\n\n"));
        form.addElement(new ElementToggle("§8» §9Level: §fAlle deine Freunde können zu jeder Zeit dein aktuelles Level einsehen.", this.getPlugin().getSettingsAPI().cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/level", "true").equals("true")));
        form.addElement(new ElementToggle("§8» §9Bargeld: §fAlle deine Freunde können zu jeder Zeit sehen, wie viel Bargeld du hast.", this.getPlugin().getSettingsAPI().cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/money", "true").equals("true")));
        form.addElement(new ElementToggle("§8» §9Spielzeit: §fAlle deine Freunde können sehen, wie viele Stunden du schon gespielt hast.", this.getPlugin().getSettingsAPI().cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/onlinetime", "true").equals("true")));
        form.addElement(new ElementToggle("§8» §9Freundschaftsanfragen: §fJeder Spieler darf dir Freundschaftsanfragen senden.", this.getPlugin().getSettingsAPI().cachedSettings.get(player.getName()).getSettings().getOrDefault("friend/requests", "true").equals("true")));

        form.onSubmit((g, h) -> {
            final boolean level = h.getToggleResponse(1);
            final boolean money = h.getToggleResponse(2);
            final boolean onlinetime = h.getToggleResponse(3);
            final boolean requests = h.getToggleResponse(4);

            this.getPlugin().getSettingsAPI().updateSettings(player.getName(), "friend/level", String.valueOf(level));
            this.getPlugin().getSettingsAPI().updateSettings(player.getName(), "friend/money", String.valueOf(money));
            this.getPlugin().getSettingsAPI().updateSettings(player.getName(), "friend/onlinetime", String.valueOf(onlinetime));
            this.getPlugin().getSettingsAPI().updateSettings(player.getName(), "friend/requests", String.valueOf(requests));

            player.sendMessage(Language.get("friend.settings.updated"));
            this.getPlugin().playSound(player, Sound.NOTE_HARP);
        });

        form.build().send(player);
    }
}
