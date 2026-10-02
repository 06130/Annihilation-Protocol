package org.lingZero.runtime;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import org.lingZero.battle.LevelDefinition;
import org.lingZero.battle.LevelState;

/**
 * 关卡状态的持久化容器：配置、状态、蓝门血量与计数器。
 */
public class LevelRuntimeData extends SavedData {
    public static final String FILE_ID = "annihilation_protocol_battle";

    private static final String KEY_DEFINITION = "definition";
    private static final String KEY_STATE = "state";
    private static final String KEY_GATE_MAX_HP = "gateMaxHp";

    private LevelDefinition definition = new LevelDefinition();
    private LevelState state = LevelState.IDLE;
    private int gateHp;
    private int gateMaxHp;
    private int remaining;
    private int alive;

    public LevelDefinition definition() {
        return definition;
    }

    public LevelState state() {
        return state;
    }

    public void setState(LevelState state) {
        this.state = state;
    }

    public int gateHp() {
        return gateHp;
    }

    public void setGateHp(int gateHp) {
        this.gateHp = Math.max(0, gateHp);
    }

    public int gateMaxHp() {
        return gateMaxHp;
    }

    public void setGateMaxHp(int gateMaxHp) {
        this.gateMaxHp = Math.max(1, gateMaxHp);
    }

    public int remaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = Math.max(0, remaining);
    }

    public int alive() {
        return alive;
    }

    public void setAlive(int alive) {
        this.alive = Math.max(0, alive);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // gateHp/remaining/alive 每局由 start()/reset() 重算、onLoaded() 归零，写盘只是放大，故不持久化。
        tag.put(KEY_DEFINITION, definition.save());
        tag.putString(KEY_STATE, state.name());
        tag.putInt(KEY_GATE_MAX_HP, gateMaxHp);
        return tag;
    }

    public static LevelRuntimeData load(CompoundTag tag, HolderLookup.Provider registries) {
        LevelRuntimeData data = new LevelRuntimeData();
        if (tag.contains(KEY_DEFINITION)) {
            data.definition = LevelDefinition.load(tag.getCompound(KEY_DEFINITION));
        }
        data.state = parseState(tag.getString(KEY_STATE));
        data.gateMaxHp = Math.max(1, tag.getInt(KEY_GATE_MAX_HP));
        return data;
    }

    private static LevelState parseState(String name) {
        for (LevelState state : LevelState.values()) {
            if (state.name().equals(name)) {
                return state;
            }
        }
        return LevelState.IDLE;
    }

    public static SavedData.Factory<LevelRuntimeData> factory() {
        return new SavedData.Factory<>(LevelRuntimeData::new, LevelRuntimeData::load);
    }
}
