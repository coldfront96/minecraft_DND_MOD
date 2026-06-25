package com.deadmind.dndmods.network;

import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UseAbilityPayload(String abilityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPayload> STREAM_CODEC =
            StreamCodec.of(UseAbilityPayload::write, UseAbilityPayload::read);

    private static UseAbilityPayload read(FriendlyByteBuf buf) {
        return new UseAbilityPayload(buf.readUtf(64));
    }

    private static void write(FriendlyByteBuf buf, UseAbilityPayload payload) {
        buf.writeUtf(payload.abilityId, 64);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Ability ability = AbilityRegistry.getAbility(abilityId);
                if (ability == null) return;

                DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
                if (ability.canUse(serverPlayer, data)) {
                    ability.execute(serverPlayer, data);
                    data.startCooldown(abilityId, ability.getCooldownTicks());
                    PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
