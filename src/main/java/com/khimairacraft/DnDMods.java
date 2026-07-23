package com.khimairacraft;

import com.khimairacraft.ability.AbilityRegistry;
import com.khimairacraft.feat.content.CompleteAdventurerFeats;
import com.khimairacraft.feat.content.CompleteArcaneFeats;
import com.khimairacraft.feat.content.CompleteChampionFeats;
import com.khimairacraft.feat.content.CompleteMageFeats;
import com.khimairacraft.feat.content.CompleteScoundrelFeats;
import com.khimairacraft.feat.content.HeroesOfHorrorFeats;
import com.khimairacraft.feat.content.TomeOfBattleFeats;
import com.khimairacraft.feat.content.TomeOfMagicFeats;
import com.khimairacraft.feat.content.CompleteDivineFeats;
import com.khimairacraft.feat.content.CompleteWarriorFeats;
import com.khimairacraft.feat.content.CoreFeats;
import com.khimairacraft.enchanting.ModMenuTypes;
import com.khimairacraft.loot.ModLootModifiers;
import com.khimairacraft.playerdata.ModAttachments;
import com.khimairacraft.race.RaceConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DnDMods.MOD_ID)
public class DnDMods {
    public static final String MOD_ID = "khimairacraft";
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
            CompleteScoundrelFeats.register();
            CompleteChampionFeats.register();
            CompleteMageFeats.register();
            HeroesOfHorrorFeats.register();
            TomeOfMagicFeats.register();
            TomeOfBattleFeats.register();
        });
    }
}
