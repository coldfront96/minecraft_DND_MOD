package com.deadmind.dndmods.playerdata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import org.jetbrains.annotations.Nullable;

public class PlayerDataSerializer implements IAttachmentSerializer<CompoundTag, DnDPlayerData> {
    @Override
    public DnDPlayerData read(CompoundTag tag) {
        DnDPlayerData data = new DnDPlayerData();
        data.load(tag);
        return data;
    }

    @Override
    public CompoundTag write(DnDPlayerData data) {
        return data.save();
    }
}
