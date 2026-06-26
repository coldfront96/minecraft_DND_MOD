package com.deadmind.dndmods.block;

import com.deadmind.dndmods.DnDMods;
import com.deadmind.dndmods.block.entity.ArcanePhylacteryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = DnDMods.MOD_ID)
public class PhylacteryEnchantingHandler {

    @SubscribeEvent
    public static void onPlayerLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.getLevels() >= 0) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ServerLevel level = serverPlayer.serverLevel();
        ChunkPos playerChunk = new ChunkPos(serverPlayer.blockPosition());

        List<ArcanePhylacteryBlockEntity> phylacteries = findPhylacteriesInChunk(level, playerChunk);
        if (phylacteries.isEmpty()) return;

        phylacteries.sort(Comparator.comparingInt(ArcanePhylacteryBlockEntity::getTier));

        int levelsToDeduct = Math.abs(event.getLevels());
        int xpCostEstimate = levelsToDeduct * 30;

        long totalAvailable = 0;
        for (ArcanePhylacteryBlockEntity p : phylacteries) {
            totalAvailable += p.getStoredXp();
        }

        if (totalAvailable <= 0) return;

        if (totalAvailable >= xpCostEstimate) {
            int remaining = xpCostEstimate;
            for (ArcanePhylacteryBlockEntity phylactery : phylacteries) {
                if (remaining <= 0) break;
                long drained = phylactery.drainXp(remaining);
                remaining -= (int) drained;
            }
            event.setLevels(0);
        } else {
            for (ArcanePhylacteryBlockEntity phylactery : phylacteries) {
                phylactery.drainXp(phylactery.getStoredXp());
            }
            int coveredLevels = (int) (totalAvailable / 30);
            int newLevelsToDeduct = levelsToDeduct - coveredLevels;
            if (newLevelsToDeduct > 0) {
                event.setLevels(-newLevelsToDeduct);
            } else {
                event.setLevels(0);
            }
        }
    }

    private static List<ArcanePhylacteryBlockEntity> findPhylacteriesInChunk(ServerLevel level, ChunkPos chunkPos) {
        List<ArcanePhylacteryBlockEntity> result = new ArrayList<>();

        if (!level.hasChunk(chunkPos.x, chunkPos.z)) return result;

        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof ArcanePhylacteryBlockEntity phylactery) {
                result.add(phylactery);
            }
        }

        return result;
    }

    public static long getTotalStoredXpInChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        List<ArcanePhylacteryBlockEntity> phylacteries = findPhylacteriesInChunk(level, chunkPos);
        long total = 0;
        for (ArcanePhylacteryBlockEntity p : phylacteries) {
            total += p.getStoredXp();
        }
        return total;
    }
}
