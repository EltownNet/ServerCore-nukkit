package net.eltown.servercore.commands.giftkeys;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementSlider;
import cn.nukkit.form.element.ElementStepSlider;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.LinkedList;

public class GiftkeyCommand extends PluginCommand<ServerCore> {

    public GiftkeyCommand(ServerCore owner) {
        super("giftkey", owner);
        this.setDescription("Erstelle und verwalte Giftkeys");
        this.setPermission("core.commands.giftkey");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            this.createKey(((Player) sender).getPlayer());
        }
        return true;
    }

    private void createKey(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Giftkey erstellen")
                .addElement(new ElementStepSlider("Bitte wähle aus, wie viel Inhalt ein Key haben soll.", Arrays.asList("1", "2", "3", "4", "5"), 1))
                .onSubmit((i, o) -> {
                    final CustomForm.Builder form1 = new CustomForm.Builder("§7» §8Giftkey erstellen");
                    form1.addElement(new ElementLabel("Reward-Formate:\n§7item;id;damage;amount;name\n§7money;amount\nlevelxp;amount\nrank;name"));
                    form1.addElement(new ElementSlider("Bitte gebe an, wie viele Spieler diesen Key einlösen können.", 1, 100, 1, 1));

                    for (int x = 0; x < Integer.parseInt(o.getStepSliderResponse(0).getElementContent()); x++) {
                        form1.addElement(new ElementInput("", "Reward " + (x + 1)));
                    }
                    form1.onSubmit((g, h) -> {
                        try {
                            final int uses = (int) h.getSliderResponse(1);
                            final StringBuilder rewards = new StringBuilder();
                            for (int x = 0; x < Integer.parseInt(o.getStepSliderResponse(0).getElementContent()); x++) {
                                rewards.append(h.getInputResponse(2 + x)).append(">:<");
                            }
                            final String rewardString = rewards.substring(0, rewards.length() - 3);

                            this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                                    case CALLBACK_NULL:
                                        player.sendMessage(Language.get("giftkey.created", delivery.getData()[1]));
                                        break;
                                }
                            }, Queue.GIFTKEYS_CALLBACK,GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(uses), rewardString);
                        } catch (final Exception e) {
                            player.sendMessage(Language.get("giftkey.invalid.input"));
                        }
                    });
                    form1.build().send(player);
                })
                .build();
        form.send(player);
    }
}