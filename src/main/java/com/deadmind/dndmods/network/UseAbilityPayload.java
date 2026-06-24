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

public record UseAbilityPayload(int slotIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UseAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("use_ability"));

    public static final StreamCodec<FriendlyByteBuf, UseAbilityPayload> STREAM_CODEC =
            StreamCodec.of(UseAbilityPayload::write, UseAbilityPayload::read);

    private static UseAbilityPayload read(FriendlyByteBuf buf) {
        return new UseAbilityPayload(buf.readVarInt());
    }

    private static void write(FriendlyByteBuf buf, UseAbilityPayload payload) {
        buf.writeVarInt(payload.slotIndex);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
                var abilities = AbilityRegistry.getAbilitiesForClass(data.getDnDClass());
                if (slotIndex >= 0 && slotIndex < abilities.size()) {
                    Ability ability = abilities.get(slotIndex);
                    if (ability.canUse(serverPlayer, data)) {
                        ability.execute(serverPlayer, data);
                        PacketDistributor.sendToPlayer(serverPlayer, SyncPlayerDataPayload.fromPlayer(data));
                    }
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
