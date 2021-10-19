package net.eltown.servercore.commands.giftkeys;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.Giftkey;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;

public class RedeemCommand extends PluginCommand<ServerCore> {

    public RedeemCommand(ServerCore owner) {
        super("redeem", owner);
        this.setDescription("Löse einen Giftkey ein");
        this.setAliases(Arrays.asList("einlösen", "key", "gutschein", "voucher").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            this.openRedeemKey(((Player) sender).getPlayer());
        }
        return true;
    }

    private void openRedeemKey(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Key einlösen")
                .addElement(new ElementInput("Bitte gebe einen Code an, um diesen einzulösen.", "XXXXX-XXXXX"))
                .onSubmit((g, h) -> {
                    final String key = h.getInputResponse(0);

                    if (key.isEmpty()) {
                        player.sendMessage(Language.get("giftkey.invalid.input"));
                        return;
                    }

                    this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                        switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                            case CALLBACK_NULL:
                                player.sendMessage(Language.get("giftkey.invalid.key", key));
                                break;
                            case CALLBACK_KEY:
                                final String[] d = delivery.getData()[1].split(">>");
                                final Giftkey giftkey = new Giftkey(d[0], Integer.parseInt(d[1]), Arrays.asList(d[2].split(">:<")), Arrays.asList(d[3].split(">:<")), Arrays.asList(d[4].split(">:<")));

                                if (giftkey.getUses().contains(player.getName())) {
                                    player.sendMessage(Language.get("giftkey.already.redeemed"));
                                    return;
                                }

                                final ModalForm modalForm = new ModalForm.Builder("§7» §8Key einlösen", "Möchtest du diesen Key einlösen und die Belohnungen, die dahinter stecken erhalten? Jeder Key kann nur einmal von dir eingelöst werden.", "§7» §aEinlösen", "§7» §cAbbrechen")
                                        .onYes(e -> {
                                            this.getPlugin().playSound(player, Sound.RANDOM_LEVELUP);
                                            this.getPlugin().getTinyRabbit().sendAndReceive(delivery1 -> {
                                                switch (GiftkeyCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                                    case CALLBACK_ALREADY_REDEEMED:
                                                        player.sendMessage(Language.get("giftkey.already.redeemed"));
                                                        break;
                                                    case CALLBACK_NULL:
                                                        player.sendMessage(Language.get("giftkey.invalid.key", key));
                                                        break;
                                                    case CALLBACK_REDEEMED:
                                                        giftkey.getRewards().forEach(reward -> {
                                                            final String[] raw = reward.split(";");
                                                            switch (raw[0]) {
                                                                case "item":
                                                                    final Item item = Item.get(Integer.parseInt(raw[1]));
                                                                    item.setDamage(Integer.parseInt(raw[2]));
                                                                    item.setCount(Integer.parseInt(raw[3]));
                                                                    item.setCustomName(raw[4]);
                                                                    player.getInventory().addItem(item);
                                                                    player.sendMessage(Language.get("giftkey.reward.item"));
                                                                    break;
                                                                case "money":
                                                                    final double amount = Double.parseDouble(raw[1]);
                                                                    Economy.getAPI().addMoney(player, amount);
                                                                    player.sendMessage(Language.get("giftkey.reward.money", amount));
                                                                    break;
                                                                case "levelxp":
                                                                    final double xp = Double.parseDouble(raw[1]);
                                                                    this.getPlugin().getLevelAPI().addExperience(player, xp);
                                                                    player.sendMessage(Language.get("giftkey.reward.xp", xp));
                                                                    break;
                                                                case "rank":
                                                                    final String rank = raw[1];
                                                                    final String unit = raw[2];
                                                                    final int time = Integer.parseInt(raw[3]);
                                                                    final long duration = this.getPlugin().getDuration(unit, time);
                                                                    this.getPlugin().getTinyRabbit().sendAndReceive((o -> {
                                                                        switch (GroupCalls.valueOf(o.getKey().toUpperCase())) {
                                                                            case CALLBACK_GROUP_DOES_NOT_EXIST:
                                                                                player.sendMessage("Fehler: RedeemCommand :: " + raw[0] + " value: " + rank);
                                                                                break;
                                                                            case CALLBACK_PLAYER_ALREADY_IN_GROUP:
                                                                                player.sendMessage(Language.get("giftkey.reward.rank.already", rank));
                                                                                break;
                                                                            case CALLBACK_SUCCESS:
                                                                                player.sendMessage(Language.get("giftkey.reward.rank", rank));
                                                                                break;
                                                                        }
                                                                    }), Queue.GROUPS, GroupCalls.REQUEST_SET_GROUP.name(), player.getName(), rank, "SYSTEM/GIFTKEY", String.valueOf(duration));
                                                                    break;
                                                                default:
                                                                    player.sendMessage("Fehler: RedeemCommand :: " + raw[0]);
                                                                    break;
                                                            }
                                                        });
                                                        break;
                                                }
                                            }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_REDEEM_KEY.name(), key, player.getName());
                                        })
                                        .onNo(e -> {
                                        })
                                        .build();
                                modalForm.send(g);
                                break;
                        }
                    }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_GET_KEY.name(), key);
                })
                .build();
        form.send(player);
    }
}