package net.eltown.servercore.commands.feature;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.level.Sound;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.listeners.EventListener;

public class StartCommand extends PluginCommand<ServerCore> {

    public StartCommand(final ServerCore owner) {
        super("start", owner);
        this.setDescription("§2Starte deine Reise auf Eltown.net");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (EventListener.needsIntroduction.contains(player.getName())) {
                EventListener.needsIntroduction.remove(player.getName());

                new ChainExecution.Builder()
                        .append(0, () -> {
                            EventListener.inIntroduction.add(player.getName());
                            player.teleport(SpawnCommand.spawnLocation);
                        })
                        .append(2, () -> {
                            this.getPlugin().getQuestAPI().setQuestOnPlayer(player.getName(), "start1");
                            this.getPlugin().getQuestAPI().getQuest("start1", quest -> {
                                this.getPlugin().playSound(player, Sound.RANDOM_LEVELUP);
                                player.sendMessage(" ");
                                player.sendMessage("§8» §fEltown §8| §7Erledige die folgenden §05 Aufgaben§7, um dein §0Startgeld §7und deine §0Startitems §7zu erhalten!");
                                player.sendMessage("§8» §fEltown §8| §fDeine erste Aufgabe lautet:");
                                player.sendMessage("§8» §fEltown §8| " + quest.getDescription());
                                player.sendMessage(" ");

                                final SimpleForm form = new SimpleForm.Builder("§7» §8Eltown", "§8» §2Info: §7Erledige die folgenden §05 Aufgaben§7, um dein §0Startgeld §7und deine §0Startitems §7zu erhalten!\n" +
                                        "\n§fDeine erste Aufgabe lautet:\n§r" + quest.getDescription() + "\n\n")
                                        .addButton(new ElementButton("§8» §aOkay"))
                                        .build();
                                form.send(player);
                            });
                        })
                        .build().start();
            }
        }
        return true;
    }
}
