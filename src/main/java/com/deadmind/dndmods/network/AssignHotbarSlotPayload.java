package com.deadmind.dndmods.network;

import com.deadmind.dndmods.ability.Ability;
import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.player.AbilityHotbar;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.PlayerDataHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AssignHotbarSlotPayload(int slotIndex, String abilityId) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/HotbarAssign");

    public static final CustomPacketPayload.Type<AssignHotbarSlotPayload> TYPE =
            new CustomPacketPayload.Type<>(ModNetwork.id("assign_hotbar_slot"));

    public static final StreamCodec<FriendlyByteBuf, AssignHotbarSlotPayload> STREAM_CODEC =
            StreamCodec.of(AssignHotbarSlotPayload::write, AssignHotbarSlotPayload::read);

    private static AssignHotbarSlotPayload read(FriendlyByteBuf buf) {
        return new AssignHotbarSlotPayload(buf.readVarInt(), buf.readUtf(64));
    }

    private static void write(FriendlyByteBuf buf, AssignHotbarSlotPayload payload) {
        buf.writeVarInt(payload.slotIndex);
        buf.writeUtf(payload.abilityId, 64);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            if (slotIndex < 0 || slotIndex >= AbilityHotbar.SLOTS) return;

            Ability ability = AbilityRegistry.getAbility(abilityId);
            if (ability == null) return;

            DnDPlayerData data = PlayerDataHelper.get(serverPlayer);

            boolean unlocked = false;
            if (data.getPrimary().getDnDClass() == ability.getRequiredClass()
                    && data.getPrimary().getLevel() >= ability.getRequiredLevel()) {
                unlocked = true;
            }
            if (!unlocked && data.getSecondary() != null
                    && data.getSecondary().getDnDClass() == ability.getRequiredClass()
                    && data.getSecondary().getLevel() >= ability.getRequiredLevel()) {
                unlocked = true;
            }
            if (!unlocked) {
                LOGGER.warn("Player {} attempted to assign ability {} without meeting requirements",
                        serverPlayer.getName().getString(), abilityId);
                PacketDistributor.sendToPlayer(serverPlayer,
                        new AbilityAssignRejectedPayload(slotIndex,
                                "Cannot assign " + ability.getName() + " — level requirement not met."));
                return;
            }

            data.getAbilityHotbar().slotAbility(slotIndex, abilityId);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
