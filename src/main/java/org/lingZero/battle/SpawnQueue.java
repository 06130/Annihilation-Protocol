package org.lingZero.battle;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成队列：只负责"何时生成"，计数由 BattleSession 负责。
 * 放在 battle 包是因为它是纯逻辑，可以被 /ap selftest 直接覆盖。
 */
public class SpawnQueue {
    private static final int DEFAULT_INTERVAL = 40;

    private final List<PendingSpawn> pending = new ArrayList<>();
    private int interval = DEFAULT_INTERVAL;

    private static final class PendingSpawn {
        private int delay;
        private int count;

        private PendingSpawn(int delay, int count) {
            this.delay = delay;
            this.count = count;
        }
    }

    public void clear() {
        pending.clear();
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }

    /** 规划一整局：首个立即生成，其余按 interval 逐个生成。 */
    public void plan(int count, int intervalTicks) {
        clear();
        this.interval = Math.max(1, intervalTicks);
        for (int i = 0; i < count; i++) {
            pending.add(new PendingSpawn(i == 0 ? 0 : this.interval, 1));
        }
    }

    /** 追加一批临时生成的敌人（/ap spawn）：首个立即，同批其余按当前间隔。 */
    public void append(int count) {
        pending.add(new PendingSpawn(0, Math.max(1, count)));
    }

    /** 推进一个 tick，返回本 tick 应生成的数量（0 或 1）。 */
    public int tickPop() {
        while (!pending.isEmpty()) {
            PendingSpawn head = pending.get(0);
            if (head.delay > 0) {
                head.delay--;
                if (head.delay > 0) {
                    return 0;
                }
            }
            if (head.count <= 0) {
                pending.remove(0);
                continue;
            }
            head.count--;
            if (head.count > 0) {
                head.delay = interval;
            } else {
                pending.remove(0);
            }
            return 1;
        }
        return 0;
    }
}
