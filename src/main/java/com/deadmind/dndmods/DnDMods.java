package com.deadmind.dndmods;

import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DnDMods.MOD_ID)
public class DnDMods {
    public static final String MOD_ID = "dndmods";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public DnDMods(IEventBus modEventBus) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);

        modEventBus.addListener(this::onCommonSetup);

        LOGGER.info("DnD Mods initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(AbilityRegistry::init);
    }
}
