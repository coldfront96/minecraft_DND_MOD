package com.deadmind.dndmods.playerdata;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public class PlayerDataSerializer implements IAttachmentSerializer<CompoundTag, DnDPlayerData> {
    @Override
    public DnDPlayerData read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
        DnDPlayerData data = new DnDPlayerData();
        data.load(tag);
        return data;
    }

    @Override
    public CompoundTag write(DnDPlayerData data, HolderLookup.Provider provider) {
        return data.save();
    }
}
