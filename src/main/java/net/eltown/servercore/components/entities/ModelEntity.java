package net.eltown.servercore.components.entities;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.FloatEntityData;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class ModelEntity extends EntityHuman {

    public ModelEntity(final FullChunk chunk, final CompoundTag nbt) {
        super(chunk, nbt);

        this.setDataProperty(new FloatEntityData(Entity.DATA_SCALE, this.namedTag.getFloat("scale")));
        this.movementSpeed = 1.0f;
    }
}
