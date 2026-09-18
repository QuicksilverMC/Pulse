package dev.rdh.pulse.spark;

import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFabricWorldInfoProvider implements WorldInfoProvider {
	protected abstract Collection<? extends World> worlds();

	@Override
	public CountsResult pollCounts() {
		int players = 0;
		int entities = 0;
		int blockEntities = 0;
		int chunks = 0;
		for (World world : worlds()) {
			players += world.players.size();
			entities += world.entities.size();
			blockEntities += world.blockEntities.size();
			chunks += world.getChunkSource().size();
		}
		return new CountsResult(players, entities, blockEntities, chunks);
	}

	@Override
	public ChunksResult<FabricChunkInfo> pollChunks() {
		ChunksResult<FabricChunkInfo> result = new ChunksResult<>();
		for (World world : worlds()) {
			Map<Long, FabricChunkInfo> chunks = new HashMap<>();
			for (Entity entity : world.entities) {
				long key = (((long) entity.chunkX) << 32) ^ (entity.chunkZ & 0xffffffffL);
				chunks.computeIfAbsent(key, ignored -> new FabricChunkInfo(entity.chunkX, entity.chunkZ)).add(entity);
			}
			result.put(world.dimension.getName(), new ArrayList<>(chunks.values()));
		}
		return result;
	}

	@Override
	public GameRulesResult pollGameRules() {
		GameRulesResult result = new GameRulesResult();
		for (World world : worlds()) {
			for (String rule : world.getGameRules().getAll()) {
				String value = world.getGameRules().get(rule);
				result.putDefault(rule, value);
				result.put(rule, world.dimension.getName(), value);
			}
		}
		return result;
	}

	@Override
	public Collection<DataPackInfo> pollDataPacks() {
		return Collections.emptyList();
	}

	@Override
	public boolean mustCallSync() {
		return true;
	}
}
