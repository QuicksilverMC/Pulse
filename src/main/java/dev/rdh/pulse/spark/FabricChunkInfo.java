package dev.rdh.pulse.spark;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import net.minecraft.entity.Entity;

import java.util.HashMap;

public final class FabricChunkInfo extends AbstractChunkInfo<Class<?>> {
    private final CountMap<Class<?>> entityCounts = new CountMap.Simple<>(new HashMap<>());

    FabricChunkInfo(int x, int z) {
        super(x, z);
    }

    void add(Entity entity) {
        this.entityCounts.increment(entity.getClass());
    }

    @Override
    public CountMap<Class<?>> getEntityCounts() {
        return this.entityCounts;
    }

    @Override
    public String entityTypeName(Class<?> type) {
        return type.getName();
    }
}
