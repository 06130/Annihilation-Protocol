package org.lingZero.battle;

import net.minecraft.nbt.CompoundTag;

/**
 * 生成队列的一个批次：等待 delayTicks 后一次性生成 count 个敌人。
 */
public record SpawnEntry(int delayTicks, int count) {
    private static final String KEY_DELAY = "delay";
    private static final String KEY_COUNT = "count";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_DELAY, delayTicks);
        tag.putInt(KEY_COUNT, count);
        return tag;
    }

    public static SpawnEntry load(CompoundTag tag) {
        return new SpawnEntry(tag.getInt(KEY_DELAY), tag.getInt(KEY_COUNT));
    }
}
