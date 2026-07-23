package com.khimairacraft.network;

import com.khimairacraft.player.AbilityHotbar;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClearHotbarSlotPayload(int slotIndex) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearHotbarSlotPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("clear_hotbar_slot"));

    public static final StreamCodec<FriendlyByteBuf, ClearHotbarSlotPayload> STREAM_CODEC =
            StreamCodec.of(ClearHotbarSlotPayload::write, ClearHotbarSlotPayload::read);

    private static ClearHotbarSlotPayload read(FriendlyByteBuf buf) {
        return new ClearHotbarSlotPayload(buf.readVarInt());
    }

    private static void write(FriendlyByteBuf buf, ClearHotbarSlotPayload payload) {
        buf.writeVarInt(payload.slotIndex);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (slotIndex < 0 || slotIndex >= AbilityHotbar.SLOTS) return;

            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);
            data.getAbilityHotbar().clearSlot(slotIndex);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
