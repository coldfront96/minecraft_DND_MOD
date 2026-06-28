package com.deadmind.dndmods;

import com.deadmind.dndmods.ability.AbilityRegistry;
import com.deadmind.dndmods.feat.content.CompleteAdventurerFeats;
import com.deadmind.dndmods.feat.content.CompleteArcaneFeats;
import com.deadmind.dndmods.feat.content.CompleteDivineFeats;
import com.deadmind.dndmods.feat.content.CompleteWarriorFeats;
import com.deadmind.dndmods.feat.content.CoreFeats;
import com.deadmind.dndmods.enchanting.ModMenuTypes;
import com.deadmind.dndmods.loot.ModLootModifiers;
import com.deadmind.dndmods.playerdata.ModAttachments;
import com.deadmind.dndmods.race.RaceConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DnDMods.MOD_ID)
public class DnDMods {
    public static final String MOD_ID = "dndmods";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public DnDMods(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        RaceConfig.register(modContainer);

        modEventBus.addListener(this::onCommonSetup);

        LOGGER.info("DnD Mods initialized");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AbilityRegistry.init();
            CoreFeats.register();
            CompleteWarriorFeats.register();
            CompleteDivineFeats.register();
            CompleteArcaneFeats.register();
            CompleteAdventurerFeats.register();
        });
    }
}
