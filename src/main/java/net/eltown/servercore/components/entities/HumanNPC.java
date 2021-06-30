package net.eltown.servercore.components.entities;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.FloatEntityData;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.network.protocol.AddPlayerPacket;

import java.nio.charset.StandardCharsets;

public class HumanNPC extends EntityHuman {

    public HumanNPC(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.setDataProperty(new FloatEntityData(DATA_SCALE, this.namedTag.getFloat("scale")));
        this.setNameTagAlwaysVisible(false);
    }

    @Override
    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId())) {
            this.hasSpawned.put(player.getLoaderId(), player);

            Skin skin = this.skin;
            skin.setTrusted(true);
            this.setSkin(skin);
            this.server.updatePlayerListData(this.getUniqueId(), this.getId(), this.getName(), this.skin, new Player[]{player});

            AddPlayerPacket pk = new AddPlayerPacket();
            pk.uuid = this.getUniqueId();
            pk.username = this.getName();
            pk.entityUniqueId = this.getId();
            pk.entityRuntimeId = this.getId();
            pk.x = (float) this.x;
            pk.y = (float) this.y;
            pk.z = (float) this.z;
            pk.speedX = (float) this.motionX;
            pk.speedY = (float) this.motionY;
            pk.speedZ = (float) this.motionZ;
            pk.yaw = (float) this.yaw;
            pk.pitch = (float) this.pitch;
            this.inventory.setItemInHand(Item.get(this.namedTag.getInt("Item")));
            pk.item = this.getInventory().getItemInHand();
            pk.metadata = this.dataProperties;
            player.dataPacket(pk);

            this.inventory.setHelmet(Item.get(this.namedTag.getInt("Helmet")));
            this.inventory.setChestplate(Item.get(this.namedTag.getInt("Chestplate")));
            this.inventory.setLeggings(Item.get(this.namedTag.getInt("Leggings")));
            this.inventory.setBoots(Item.get(this.namedTag.getInt("Boots")));

            this.inventory.sendArmorContents(player);

            this.server.removePlayerListData(this.getUniqueId(), new Player[]{player});

            super.spawnTo(player);
        }
    }

    public String getNpcID() {
        return this.namedTag.getString("npc_id");
    }

    public void setID(final String id) {
        this.namedTag.putString("npc_id", id);
    }

    public static CompoundTag createNBT(Player p, String name, String id) {
        CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", p.x))
                        .add(new DoubleTag("", p.y))
                        .add(new DoubleTag("", p.z)))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float) p.getYaw()))
                        .add(new FloatTag("", (float) p.getPitch())))
                .putBoolean("Invulnerable", true)
                .putString("NameTag", name)
                .putList(new ListTag<StringTag>("Commands"))
                .putList(new ListTag<StringTag>("PlayerCommands"))
                .putBoolean("npc", true)
                .putFloat("scale", 1);
        CompoundTag skinTag = new CompoundTag()
                .putByteArray("Data", p.getSkin().getSkinData().data)
                .putInt("SkinImageWidth", p.getSkin().getSkinData().width)
                .putInt("SkinImageHeight", p.getSkin().getSkinData().height)
                .putString("ModelId", p.getSkin().getSkinId())
                .putString("CapeId", p.getSkin().getCapeId())
                .putByteArray("CapeData", p.getSkin().getCapeData().data)
                .putInt("CapeImageWidth", p.getSkin().getCapeData().width)
                .putInt("CapeImageHeight", p.getSkin().getCapeData().height)
                .putByteArray("SkinResourcePatch", p.getSkin().getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", p.getSkin().getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", p.getSkin().getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", p.getSkin().isPremium())
                .putBoolean("PersonaSkin", p.getSkin().isPersona())
                .putBoolean("CapeOnClassicSkin", p.getSkin().isCapeOnClassic());
        nbt.putCompound("Skin", skinTag);
        nbt.putBoolean("ishuman", true);
        nbt.putInt("Item", p.getInventory().getItemInHand().getId());
        nbt.putInt("Helmet", p.getInventory().getHelmet().getId());
        nbt.putInt("Chestplate", p.getInventory().getChestplate().getId());
        nbt.putInt("Leggings", p.getInventory().getLeggings().getId());
        nbt.putInt("Boots", p.getInventory().getBoots().getId());
        nbt.putString("npc_id", id);
        return nbt;
    }

}
