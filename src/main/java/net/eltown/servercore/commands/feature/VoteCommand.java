package net.eltown.servercore.commands.feature;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.level.Sound;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class VoteCommand extends PluginCommand<ServerCore> {

    public VoteCommand(final ServerCore owner) {
        super("vote", owner);
        this.setDescription("Stimme für unseren Server ab und erhalte tolle Belohnungen");
        this.setAliases(Collections.singletonList("abstimmen").toArray(new String[]{}));
    }

    final Cooldown cooldown = new Cooldown(TimeUnit.SECONDS.toMillis(5));

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;

            if (!this.cooldown.hasCooldown(player.getName())) {
                this.getPlugin().getVote(player.getName(), code -> {
                    switch (code) {
                        case "0":
                            player.sendMessage(Language.get("vote.not.voted"));
                            this.getPlugin().playSound(player, Sound.NOTE_BASS);
                            break;
                        case "1":
                            this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                                    case CALLBACK_NULL:
                                        player.sendMessage(Language.get("vote.successful.voted", delivery.getData()[1]));
                                        this.getPlugin().playSound(player, Sound.RANDOM_LEVELUP);
                                        this.getPlugin().setVoted(player.getName());

                                        this.getPlugin().getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_BROADCAST_PROXY_MESSAGE.name(), Language.get("vote.vote.broadcast", player.getName()));
                                        break;
                                }
                            }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(1), "levelxp;200>:<money;25>:<crate;common;2", player.getName());
                            break;
                        case "2":
                            player.sendMessage(Language.get("vote.already.voted"));
                            this.getPlugin().playSound(player, Sound.NOTE_BASS);
                            break;
                    }
                });
            } else {
                player.sendMessage(Language.get("vote.do.not.spam"));
                this.getPlugin().playSound(player, Sound.NOTE_BASS);
            }
        }
        return true;
    }

}
