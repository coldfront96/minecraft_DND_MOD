package com.deadmind.dndmods.network;

import com.deadmind.dndmods.classes.DnDClass;
import com.deadmind.dndmods.playerdata.AbilityScores;
import com.deadmind.dndmods.playerdata.ClassEntry;
import com.deadmind.dndmods.playerdata.DnDPlayerData;
import com.deadmind.dndmods.playerdata.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record LevelUpPayload(
        boolean advancePrimary,
        boolean addingNewClass,
        String newSecondaryClassName,
        int asiScore1,
        int asiScore2,
        boolean featAcknowledged
) implements CustomPacketPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger("DnDMods/LevelUp");

    public static final Type<LevelUpPayload> TYPE =
            new Type<>(ModNetwork.id("level_up"));

    public static final StreamCodec<FriendlyByteBuf, LevelUpPayload> STREAM_CODEC =
            StreamCodec.of(LevelUpPayload::write, LevelUpPayload::read);

    private static LevelUpPayload read(FriendlyByteBuf buf) {
        return new LevelUpPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(32),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    private static void write(FriendlyByteBuf buf, LevelUpPayload payload) {
        buf.writeBoolean(payload.advancePrimary);
        buf.writeBoolean(payload.addingNewClass);
        buf.writeUtf(payload.newSecondaryClassName, 32);
        buf.writeInt(payload.asiScore1);
        buf.writeInt(payload.asiScore2);
        buf.writeBoolean(payload.featAcknowledged);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            DnDPlayerData data = player.getData(ModAttachments.PLAYER_DATA);

            if (!data.isLevelUpAvailable()) {
                LOGGER.warn("Player {} attempted level up without it being available", player.getName().getString());
                return;
            }

            if (data.getTotalLevel() >= DnDPlayerData.MAX_LEVEL) {
                LOGGER.warn("Player {} attempted level up at max level", player.getName().getString());
                return;
            }

            if (addingNewClass) {
                if (!data.canMulticlass()) {
                    LOGGER.warn("Player {} cannot multiclass", player.getName().getString());
                    return;
                }
                if (data.getPrestigeClass() != com.deadmind.dndmods.classes.PrestigeClass.NONE) {
                    LOGGER.warn("Player {} has a prestige class and cannot add secondary", player.getName().getString());
                    return;
                }

                DnDClass newClass;
                try {
                    newClass = DnDClass.valueOf(newSecondaryClassName);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Player {} sent invalid class name: {}", player.getName().getString(), newSecondaryClassName);
                    return;
                }

                if (newClass == DnDClass.NONE || newClass == data.getDnDClass()) {
                    LOGGER.warn("Player {} tried to multiclass into invalid class: {}", player.getName().getString(), newClass);
                    return;
                }

                int oldMaxHp = data.getMaxHp();
                data.setSecondaryClass(newClass);
                int newMaxHp = data.getMaxHp();
                data.setCurrentHp(data.getCurrentHp() + (newMaxHp - oldMaxHp));

                data.clearLevelUpAvailable();

                LOGGER.info("Player {} added secondary class: {}", player.getName().getString(), newClass.getDisplayName());
            } else {
                ClassEntry advancing = advancePrimary ? data.getPrimary() : data.getSecondary();
                if (advancing == null || advancing.getDnDClass() == DnDClass.NONE) {
                    LOGGER.warn("Player {} tried to advance null/NONE class", player.getName().getString());
                    return;
                }

                int currentLevel = advancing.getLevel();
                int newLevel = currentLevel + 1;

                int oldMaxHp = data.getMaxHp();
                advancing.setLevel(newLevel);
                int newMaxHp = data.getMaxHp();
                data.setCurrentHp(data.getCurrentHp() + (newMaxHp - oldMaxHp));
                advancing.setCurrentResource(advancing.getMaxResource());

                if (newLevel % 4 == 0) {
                    if (asiScore1 < 0 || asiScore1 > 5) {
                        LOGGER.warn("Player {} sent invalid ASI score1: {}", player.getName().getString(), asiScore1);
                        advancing.setLevel(currentLevel);
                        return;
                    }
                    AbilityScores scores = data.getAbilityScores();
                    if (asiScore2 >= 0 && asiScore2 <= 5 && asiScore2 != asiScore1) {
                        applyAsiPoint(scores, asiScore1, 1);
                        applyAsiPoint(scores, asiScore2, 1);
                    } else {
                        applyAsiPoint(scores, asiScore1, 2);
                    }
                }

                int totalLevel = data.getTotalLevel();
                int[] featLevels = {1, 3, 6, 9, 12, 15, 18, 20};
                for (int fl : featLevels) {
                    if (totalLevel == fl) {
                        data.addFeatSlot();
                        LOGGER.info("Player {} gained a feat slot at level {}", player.getName().getString(), totalLevel);
                        break;
                    }
                }

                data.clearLevelUpAvailable();

                LOGGER.info("Player {} leveled up {} to level {}",
                        player.getName().getString(),
                        advancing.getDnDClass().getDisplayName(),
                        newLevel);
            }

            data.resetXpAfterLevelUp();

            // Dwarven Toughness grants +1 HP per character level — rescale to
            // the new total level so FeatEffectHandler applies the right bonus.
            if (data.hasFeat("dwarven_toughness")) {
                data.setDwarvenToughnessHp(data.getTotalLevel());
            }

            // Battle Hardened (Complete Warrior) grants +2 max HP per hit die —
            // rescale to the new total level so FeatEffectHandler applies the
            // right bonus on the next tick.
            if (data.hasFeat("battle_hardened")) {
                data.setBattleHardenedHp(data.getTotalLevel() * 2);
            }

            // Improved Toughness (Complete Warrior) grants +1 HP per character
            // level via the shared toughness stacking system — gaining a level
            // adds one more stack so the MAX_HEALTH reconcile keeps pace.
            if (data.hasFeat("improved_toughness")) {
                data.setToughnessFeatCount(data.getToughnessFeatCount() + 1);
            }

            // Blade of Force (Complete Warrior) deals flat force damage equal to
            // the INT modifier — recalculate in case an ASI changed INT.
            if (data.hasFeat("blade_of_force")) {
                data.setBladeOfForceBonus(data.getAbilityScores().getIntMod());
            }

            // Holy Warrior (Complete Divine) deals flat melee damage equal to the
            // WIS modifier — recalculate in case an ASI changed WIS.
            if (data.hasFeat("holy_warrior")) {
                data.setHolyWarriorDamageBonus(data.getAbilityScores().getWisMod());
            }

            // Arcane Toughness (Complete Arcane) grants +1 HP per Wizard level —
            // rescale to the new Wizard level so FeatEffectHandler applies the
            // right bonus on the next tick.
            if (data.hasFeat("arcane_toughness")) {
                data.setArcaneToughnessHp(data.getClassLevel(DnDClass.WIZARD));
            }

            data.getAbilityHotbar().validateAndClean(data);

            PacketDistributor.sendToPlayer(player, SyncPlayerDataPayload.fromPlayer(data));
        });
    }

    private static void applyAsiPoint(AbilityScores scores, int index, int amount) {
        switch (index) {
            case 0 -> scores.setStrength(scores.getStrength() + amount);
            case 1 -> scores.setDexterity(scores.getDexterity() + amount);
            case 2 -> scores.setConstitution(scores.getConstitution() + amount);
            case 3 -> scores.setIntelligence(scores.getIntelligence() + amount);
            case 4 -> scores.setWisdom(scores.getWisdom() + amount);
            case 5 -> scores.setCharisma(scores.getCharisma() + amount);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
