package net.eltown.servercore.commands.friends;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.level.Sound;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.friends.FriendCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
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
                            .addButton(new ElementButton("§8» §9Deine Freunde §8[§f" + friends.size() + "§8]", new ElementButtonImageData("url", "")), e -> {})
                            .addButton(new ElementButton("§8» §9Offene Anfragen §8[§f" + requests.size() + "§8]", new ElementButtonImageData("url", "")), e -> {})
                            .addButton(new ElementButton("§8» §9Freund hinzufügen", new ElementButtonImageData("url", "")), e -> this.openAddFriend(player, finalFriends, finalRequests))
                            .addButton(new ElementButton("§8» §9Einstellungen", new ElementButtonImageData("url", "")), e -> {})
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
                    final List<String> players = new ArrayList<>(Collections.singletonList("KEINER"));
                    players.addAll(Arrays.asList(delivery.getData()[1].split("#")));
                    players.remove(player.getName());
                    final CustomForm form = new CustomForm.Builder("§7» §8Freund hinzufügen")
                            .addElement(new ElementDropdown("§8» §fWähle einen Spieler, der grade online ist, aus, um diesem eine Freundschaftsanfrage zu senden.", players))
                            .addElement(new ElementInput("§8» §fAlternativ kannst du auch einen Spieler angeben, der aktuell nicht online ist.", "HanzFranz"))
                            .onSubmit((g, h) -> {
                                final String drop = h.getDropdownResponse(0).getElementContent();
                                final String input = h.getInputResponse(1);

                                String target = "";
                                if (!drop.equals("KEINER")) {
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
                                    player.sendMessage(Language.get("friend.friendship.created", target));
                                    this.getPlugin().playSound(player, Sound.NOTE_HARP);
                                    return;
                                }

                                //check exists

                                this.getPlugin().getTinyRabbit().send(Queue.FRIEND_RECEIVE, FriendCalls.REQUEST_CREATE_FRIEND_REQUEST.name(), player.getName(), target);
                                player.sendMessage(Language.get("friend.request.created", target));
                                this.getPlugin().playSound(player, Sound.NOTE_HARP);
                            })
                            .build();
                    form.send(player);
                    break;
            }
        }, Queue.CORE_CALLBACK, CoreCalls.REQUEST_GET_ONLINE_PLAYERS.name());
    }
}
