package com.khimairacraft.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

public class WitchFragmentLootModifier extends LootModifier {

    public static final MapCodec<WitchFragmentLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
                    .apply(inst, WitchFragmentLootModifier::new));

    public WitchFragmentLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    @NotNull
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!context.hasParam(LootContextParams.THIS_ENTITY)) return generatedLoot;

        var entity = context.getParam(LootContextParams.THIS_ENTITY);
        if (entity.getType() != EntityType.WITCH) return generatedLoot;

        if (context.getRandom().nextFloat() < 0.15f) {
            int count = 1 + context.getRandom().nextInt(3);
            generatedLoot.add(new ItemStack(com.khimairacraft.ModItems.ARCANE_ORE_FRAGMENT.get(), count));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
