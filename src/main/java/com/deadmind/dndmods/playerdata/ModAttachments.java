package com.deadmind.dndmods.playerdata;

import com.deadmind.dndmods.DnDMods;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, DnDMods.MOD_ID);

    public static final Supplier<AttachmentType<DnDPlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () ->
                    AttachmentType.builder(DnDPlayerData::new)
                            .serialize(new PlayerDataSerializer())
                            .copyOnDeath()
                            .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
