package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntityFurnace;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.inventory.FurnaceSmeltEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.furnace.Furnace;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FurnaceListener implements Listener {

    private final ServerCore serverCore;

    @EventHandler
    public void on(final FurnaceSmeltEvent event) {
        final BlockEntityFurnace blockEntityFurnace = event.getFurnace();
        final Item result = event.getResult();

        final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(blockEntityFurnace.getLocation());
        if (furnace != null) {
            if (furnace.getFurnaceLevel().getLevel() != 0) {
                blockEntityFurnace.setCookTime((200 * (furnace.getFurnaceLevel().getSmeltingPercent() + furnace.getSmeltingPercent()) / 100) + 200);
                boolean b = Math.random() * 100 <= (furnace.getFurnaceLevel().getDoublePercent() + furnace.getDoublePercent());
                if (event.getResult().count > 61) b = false;
                if (b) result.setCount(event.getResult().count + 1);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (event.getAction() == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            if (block.getId() == BlockID.FURNACE || block.getId() == BlockID.BURNING_FURNACE) {
                final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
                if (furnace != null) {
                    if (furnace.getOwner().equals(player.getName())) {
                        final Furnace.FurnaceLevel nextLevel = this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(furnace.getFurnaceLevel().getLevel() + 1);
                        if (nextLevel != null) {
                            final ModalForm form = new ModalForm.Builder("§7» §8Ofen upgraden",
                                    "§8» §9Ofen Level: §f" + furnace.getFurnaceLevel().getLevel() + "\n§8» §9Geschwindigkeit: §f" + furnace.getFurnaceLevel().getSmeltingPercent() + " Prozent §7[§a+ §f" +
                                            furnace.getSmeltingPercent() + " Prozent§7]\n§8» §9Doppelter Ertrag: §f" + furnace.getFurnaceLevel().getDoublePercent() + " Prozent " +
                                            "§7[§a+ §f" + furnace.getDoublePercent() + " Prozent§7]\n\n§f§lNächstes erhältliches Level:§r\n§8» §cOfen Level: §f" + nextLevel.getLevel() + "" +
                                            "\n§8» §cGeschwindigkeit: §f" + nextLevel.getSmeltingPercent() + " Prozent\n§8» §cDoppelter Ertrag: §f" + nextLevel.getDoublePercent() + " Prozent " +
                                            "\n\n§8» §cBenötigtes Level: §f" + nextLevel.getLevelValue() + "\n§8» §cKosten: §f$" + Economy.getAPI().getMoneyFormat().format(nextLevel.getValue()) + "\n\n",
                                    "§8» §aUpgraden", "§8» §cAbbrechen")
                                    .onYes(e -> {
                                        if (this.serverCore.getLevelAPI().getLevel(player.getName()).getLevel() >= nextLevel.getLevelValue()) {
                                            Economy.getAPI().getMoney(player, money -> {
                                                if (money >= nextLevel.getValue()) {
                                                    Economy.getAPI().reduceMoney(player, nextLevel.getValue());
                                                    this.serverCore.getFurnaceAPI().upgradeFurnace(furnace, nextLevel);
                                                    player.sendMessage(Language.get("furnace.upgrade.success", nextLevel.getLevel()));
                                                    this.serverCore.playSound(player, Sound.RANDOM_LEVELUP);
                                                } else {
                                                    player.sendMessage(Language.get("furnace.upgrade.need.money"));
                                                    this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                }
                                            });
                                        } else {
                                            player.sendMessage(Language.get("furnace.upgrade.need.level", nextLevel.getLevelValue()));
                                            this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                        }
                                    })
                                    .onNo(e -> {
                                    })
                                    .build();
                            form.send(player);
                        } else {
                            final ModalForm form = new ModalForm.Builder("§7» §8Ofen upgraden",
                                    "§8» §9Ofen Level: §f" + furnace.getFurnaceLevel().getLevel() + "\n§8» §9Geschwindigkeit: §f" + furnace.getFurnaceLevel().getSmeltingPercent() + " Prozent §7[§a+ §f" +
                                            furnace.getSmeltingPercent() + " Prozent§7]\n§8» §9Doppelter Ertrag: §f" + furnace.getFurnaceLevel().getDoublePercent() + " Prozent " +
                                            "§7[§a+ §f" + furnace.getDoublePercent() + " Prozent§7]\n\n§f§lNächstes erhältliches Level:§r\n§8» §cDein Ofen hat das maximale Level \nerreicht. Du kannst" +
                                            " allerdings weitere Tuningbauteile anbringen.\n\n",
                                    "§8» §aUpgraden", "§8» §cAbbrechen")
                                    .onYes(e -> {
                                    })
                                    .onNo(e -> {
                                    })
                                    .build();
                            form.send(player);
                        }
                    } else {

                    }
                }
            }
        }
    }

    final Cooldown placeCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(7));

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final Item item = event.getItem();

        if (item.getId() == BlockID.FURNACE) {
            if (!this.placeCooldown.hasCooldown(player.getName())) {
                if (item.getNamedTag() == null) {
                    this.serverCore.getFurnaceAPI().placeFurnace(player.getName(), block.getLocation());
                } else {
                    final Furnace furnace = new Furnace(null, -1, null, this.serverCore.getFurnaceAPI().cachedFurnaceLevel.get(item.getNamedTag().getInt("data.level")),
                            item.getNamedTag().getInt("data.smelting"), item.getNamedTag().getInt("data.double"));
                    this.serverCore.getFurnaceAPI().placeFurnace(player.getName(), block.getLocation(), furnace);
                }
            } else {
                player.sendActionBar("§cBitte warte einen Moment.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (block.getId() == BlockID.FURNACE || block.getId() == BlockID.BURNING_FURNACE) {
            final Furnace furnace = this.serverCore.getFurnaceAPI().cachedFurnaces.get(block.getLocation());
            if (furnace != null) {
                if (player.getGamemode() == 1) {
                    if (player.isSneaking()) {
                        this.serverCore.getFurnaceAPI().breakFurnace(furnace);

                        final Item item = Item.get(BlockID.FURNACE, 0, 1);
                        item.setNamedTag(new CompoundTag()
                                .putInt("data.smelting", furnace.getSmeltingPercent())
                                .putInt("data.double", furnace.getSmeltingPercent())
                                .putInt("data.level", furnace.getFurnaceLevel().getLevel())
                        );
                        item.setLore(
                                "§r§8» §bLevel: §7" + furnace.getFurnaceLevel().getLevel(),
                                " ",
                                "§r§fZusätzliche Werte:",
                                "§r§8» §bBrenngeschwindigkeit: §a+ §7" + furnace.getSmeltingPercent() + "%",
                                "§r§8» §bDoppelter Ertrag: §a+ §7" + furnace.getDoublePercent() + "%"
                        );
                        player.getInventory().addItem(item);

                        player.sendMessage(Language.get("furnace.break.success", furnace.getFurnaceLevel().getLevel()));
                        this.serverCore.playSound(player, Sound.RANDOM_ORB);
                    } else {
                        player.sendMessage(Language.get("furnace.break.need.sneak"));
                        this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                        event.setCancelled(true);
                    }
                } else {
                    this.serverCore.getFurnaceAPI().breakFurnace(furnace);
                    for (final Item item : event.getDrops()) {
                        if (item.getId() == BlockID.FURNACE) {
                            item.setNamedTag(new CompoundTag()
                                    .putInt("data.smelting", furnace.getSmeltingPercent())
                                    .putInt("data.double", furnace.getSmeltingPercent())
                                    .putInt("data.level", furnace.getFurnaceLevel().getLevel())
                            );
                            item.setLore(
                                    "§r§bLevel: §7" + furnace.getFurnaceLevel().getLevel(),
                                    " ",
                                    "§r§fZusätzliche Werte:",
                                    "§r§bBrenngeschwindigkeit: §a+ §7" + furnace.getSmeltingPercent() + "%",
                                    "§r§bDoppelter Ertrag: §a+ §7" + furnace.getDoublePercent() + "%"
                            );

                            player.sendMessage(Language.get("furnace.break.success", furnace.getFurnaceLevel().getLevel()));
                            this.serverCore.playSound(player, Sound.RANDOM_ORB);
                        }
                    }
                }
            }
        }
    }

}
