package org.lingZero.battle;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 关卡配置：红门、蓝门、可部署区域、固定路径点、生成队列。
 * 只描述数据，不驱动任何实体。
 */
public class LevelDefinition {
    private static final String KEY_SPAWN = "spawn";
    private static final String KEY_GATE = "gate";
    private static final String KEY_AREA_MIN = "areaMin";
    private static final String KEY_AREA_MAX = "areaMax";
    private static final String KEY_PATH = "path";
    /** 路径阻挡半径：方块中心与路径段的 XZ 距离小于该值即视为在路径上。 */
    public static final double PATH_RADIUS = 0.5;

    @Nullable
    private BlockPos spawnPos;
    @Nullable
    private BlockPos gatePos;
    @Nullable
    private BlockPos areaMin;
    @Nullable
    private BlockPos areaMax;
    private final List<BlockPos> path = new ArrayList<>();

    @Nullable
    public BlockPos spawnPos() {
        return spawnPos;
    }

    public void setSpawnPos(BlockPos pos) {
        this.spawnPos = pos.immutable();
    }

    @Nullable
    public BlockPos gatePos() {
        return gatePos;
    }

    public void setGatePos(BlockPos pos) {
        this.gatePos = pos.immutable();
    }

    public void setArea(BlockPos a, BlockPos b) {
        this.areaMin = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        this.areaMax = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public List<BlockPos> path() {
        return List.copyOf(path);
    }

    public void addPathPoint(BlockPos pos) {
        path.add(pos.immutable());
    }

    public void clearPath() {
        path.clear();
    }

    /** 配置缺失项，用于 /ap start 的失败提示。 */
    public List<String> missingSetup() {
        List<String> missing = new ArrayList<>();
        if (spawnPos == null) {
            missing.add("spawn");
        }
        if (gatePos == null) {
            missing.add("gate");
        }
        if (areaMin == null || areaMax == null) {
            missing.add("area");
        }
        return missing;
    }

    public boolean isConfigured() {
        return missingSetup().isEmpty();
    }

    public boolean areaContains(BlockPos pos) {
        if (areaMin == null || areaMax == null) {
            return false;
        }
        return pos.getX() >= areaMin.getX() && pos.getX() <= areaMax.getX()
                && pos.getY() >= areaMin.getY() && pos.getY() <= areaMax.getY()
                && pos.getZ() >= areaMin.getZ() && pos.getZ() <= areaMax.getZ();
    }

    /** 路径段列表（红门 -> 路径点 -> 蓝门），用于部署判定与调试。 */
    public List<BlockPos> pathNodes() {
        List<BlockPos> nodes = new ArrayList<>();
        if (spawnPos != null) {
            nodes.add(spawnPos);
        }
        nodes.addAll(path);
        if (gatePos != null) {
            nodes.add(gatePos);
        }
        return nodes;
    }

    public boolean isOnPath(BlockPos pos) {
        List<BlockPos> nodes = pathNodes();
        if (nodes.size() < 2) {
            return false;
        }
        double px = pos.getX() + 0.5;
        double pz = pos.getZ() + 0.5;
        for (int i = 0; i + 1 < nodes.size(); i++) {
            BlockPos a = nodes.get(i);
            BlockPos b = nodes.get(i + 1);
            if (distanceToSegmentXZ(px, pz, a.getX() + 0.5, a.getZ() + 0.5, b.getX() + 0.5, b.getZ() + 0.5) < PATH_RADIUS) {
                return true;
            }
        }
        return false;
    }

    static double distanceToSegmentXZ(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSqr = dx * dx + dz * dz;
        if (lengthSqr < 1.0E-6) {
            return Math.hypot(px - ax, pz - az);
        }
        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        return Math.hypot(px - (ax + t * dx), pz - (az + t * dz));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (spawnPos != null) {
            tag.put(KEY_SPAWN, writePos(spawnPos));
        }
        if (gatePos != null) {
            tag.put(KEY_GATE, writePos(gatePos));
        }
        if (areaMin != null && areaMax != null) {
            tag.put(KEY_AREA_MIN, writePos(areaMin));
            tag.put(KEY_AREA_MAX, writePos(areaMax));
        }
        ListTag pathTag = new ListTag();
        for (BlockPos pos : path) {
            pathTag.add(writePos(pos));
        }
        tag.put(KEY_PATH, pathTag);
        return tag;
    }

    public static LevelDefinition load(CompoundTag tag) {
        LevelDefinition definition = new LevelDefinition();
        definition.spawnPos = readPos(tag.get(KEY_SPAWN));
        definition.gatePos = readPos(tag.get(KEY_GATE));
        definition.areaMin = readPos(tag.get(KEY_AREA_MIN));
        definition.areaMax = readPos(tag.get(KEY_AREA_MAX));
        ListTag pathTag = tag.getList(KEY_PATH, Tag.TAG_INT_ARRAY);
        for (int i = 0; i < pathTag.size(); i++) {
            BlockPos pos = readPos(pathTag.get(i));
            if (pos != null) {
                definition.path.add(pos);
            }
        }
        return definition;
    }

    private static Tag writePos(BlockPos pos) {
        return new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    @Nullable
    private static BlockPos readPos(@Nullable Tag tag) {
        if (tag instanceof IntArrayTag array && array.size() == 3) {
            int[] values = array.getAsIntArray();
            return new BlockPos(values[0], values[1], values[2]);
        }
        return null;
    }
}
