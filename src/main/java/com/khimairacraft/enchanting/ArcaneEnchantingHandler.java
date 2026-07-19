package com.khimairacraft.enchanting;

import com.khimairacraft.classes.DnDClass;
import com.khimairacraft.items.enchanting.ArcaneDust;
import com.khimairacraft.items.enchanting.DustTier;
import com.khimairacraft.playerdata.DnDPlayerData;
import com.khimairacraft.playerdata.ModAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class ArcaneEnchantingHandler {

    public record EnchantResult(boolean allowed, String message, float powerReduction, float upcastBonus) {
        public static EnchantResult denied(String message) {
            return new EnchantResult(false, message, 0f, 0f);
        }
        public static EnchantResult allowed(float powerReduction, float upcastBonus) {
            return new EnchantResult(true, null, powerReduction, upcastBonus);
        }
    }

    public static EnchantResult canEnchant(ServerPlayer player, String enchantmentName, int enchantLevel, ItemStack dustStack) {
        DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);
        DnDClass dndClass = data.getDnDClass();

        if (dndClass == DnDClass.FIGHTER || dndClass == DnDClass.ROGUE
                || dndClass == DnDClass.BARBARIAN || dndClass == DnDClass.NONE) {
            return EnchantResult.denied("You lack the arcane knowledge to use this.");
        }

        if (dndClass == DnDClass.RANGER && !RangerEnchantmentList.isAllowed(enchantmentName)) {
            return EnchantResult.denied("Your connection to nature does not extend to this.");
        }

        if (!(dustStack.getItem() instanceof ArcaneDust arcaneDust)) {
            return EnchantResult.denied("Arcane dust is required.");
        }

        DustTier dustTier = arcaneDust.getTier();

        if (dustTier == DustTier.NETHER_STAR && data.getTotalLevel() < 20) {
            return EnchantResult.denied("Epic enchantments require mastery beyond mortal limits.");
        }

        int playerSpellLevel = EnchantmentSpellLevel.getPlayerSpellLevel(data.getLevel(), data.getTotalLevel());
        int enchantSpellLevel = EnchantmentSpellLevel.getSpellLevel(enchantmentName, enchantLevel);

        if (enchantSpellLevel > playerSpellLevel + 2) {
            return EnchantResult.denied("Your magic is not yet strong enough for this.");
        }

        float powerReduction = 0f;
        if (enchantSpellLevel > playerSpellLevel) {
            powerReduction = 0.4f;
        }

        DustTier minimumTier = EnchantmentSpellLevel.getMinimumDustTier(enchantmentName, enchantLevel);
        float upcastBonus = 0f;
        if (dustTier.getTier() > minimumTier.getTier()) {
            upcastBonus = Math.min((dustTier.getTier() - minimumTier.getTier()) * 0.15f, 0.75f);
        }

        return EnchantResult.allowed(powerReduction, upcastBonus);
    }

    public static void applyModifiers(ItemStack enchantedItem, float powerReduction, float upcastBonus) {
        if (powerReduction > 0f || upcastBonus > 0f) {
            CustomData existing = enchantedItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = existing.copyTag();
            if (powerReduction > 0f) {
                tag.putFloat("khimairacraft:ArcanePowerReduction", powerReduction);
            }
            if (upcastBonus > 0f) {
                tag.putFloat("khimairacraft:ArcaneUpcastBonus", upcastBonus);
            }
            enchantedItem.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
}
