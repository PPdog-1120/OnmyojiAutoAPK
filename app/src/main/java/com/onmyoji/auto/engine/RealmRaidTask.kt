package com.onmyoji.auto.engine
import kotlinx.coroutines.delay
import android.content.Context
import com.onmyoji.auto.model.TaskConfig

/**
 * 个人突破任务 — 保留 OAS 核心逻辑
 *
 * 流程：导航 → 勋章选择 → 挑战 → 继续/刷新 → 退出 → 设置探索恢复
 */
class RealmRaidTask(
    context: Context,
    device: DeviceController,
    config: TaskConfig
) : BaseTask(context, device, config) {

    // ========== 资源定义 ==========
    private val I_BACK_RED = RuleImage("back_red",
        "realm_raid/res_back_red.png",
        intArrayOf(1178, 101, 57, 64), intArrayOf(1178, 101, 57, 64), 0.7f)

    private val I_FIRE = RuleImage("fire",
        "realm_raid/res_fire.png",
        intArrayOf(982, 494, 136, 63), intArrayOf(140, 129, 1024, 584), 0.8f)

    private val I_LOCK = RuleImage("lock",
        "realm_raid/res_lock.png",
        intArrayOf(818, 579, 36, 41), intArrayOf(818, 579, 36, 41), 0.8f)

    private val I_UNLOCK = RuleImage("unlock",
        "realm_raid/res_unlock.png",
        intArrayOf(818, 579, 38, 42), intArrayOf(818, 579, 38, 42), 0.8f)

    private val I_FRESH = RuleImage("fresh",
        "realm_raid/res_fresh.png",
        intArrayOf(957, 564, 182, 66), intArrayOf(957, 564, 182, 66), 0.8f)

    private val I_FRESH_ENSURE = RuleImage("fresh_ensure",
        "realm_raid/res_fresh_ensure.png",
        intArrayOf(672, 403, 173, 59), intArrayOf(672, 403, 173, 59), 0.8f)

    private val I_SOUL_RAID = RuleImage("soul_raid",
        "realm_raid/res_soul_raid.png",
        intArrayOf(577, 502, 100, 100), intArrayOf(577, 502, 100, 100), 0.8f)

    private val I_RR_PERSON = RuleImage("rr_person",
        "realm_raid/res_rr_person.png",
        intArrayOf(1203, 236, 56, 100), intArrayOf(1203, 236, 56, 155), 0.8f)

    private val I_RR_THREE = RuleImage("rr_three",
        "realm_raid/res_rr_three.png",
        intArrayOf(346, 584, 42, 44), intArrayOf(346, 584, 42, 44), 0.8f)

    private val I_FROG_RAID = RuleImage("frog_raid",
        "realm_raid/res_frog_raid.png",
        intArrayOf(511, 301, 282, 100), intArrayOf(511, 301, 282, 100), 0.8f)

    // 勋章
    private val I_MEDAL_5 = RuleImage("medal_5",
        "realm_raid/res_medal_5.png",
        intArrayOf(238, 205, 212, 53), intArrayOf(216, 187, 919, 364), 0.8f)

    private val I_MEDAL_4 = RuleImage("medal_4",
        "realm_raid/res_medal_4.png",
        intArrayOf(241, 483, 193, 46), intArrayOf(228, 178, 899, 362), 0.8f)

    private val I_MEDAL_3 = RuleImage("medal_3",
        "realm_raid/res_medal_3.png",
        intArrayOf(240, 210, 193, 41), intArrayOf(229, 189, 894, 345), 0.8f)

    private val I_MEDAL_2 = RuleImage("medal_2",
        "realm_raid/res_medal_2.png",
        intArrayOf(572, 478, 198, 48), intArrayOf(217, 193, 923, 354), 0.8f)

    private val I_MEDAL_1 = RuleImage("medal_1",
        "realm_raid/res_medal_1.png",
        intArrayOf(570, 206, 199, 52), intArrayOf(237, 198, 892, 336), 0.8f)

    private val I_MEDAL_0 = RuleImage("medal_0",
        "realm_raid/res_medal_0.png",
        intArrayOf(536, 336, 176, 51), intArrayOf(231, 200, 898, 336), 0.8f)

    // 9宫格分区
    private data class Partition(val x: Int, val y: Int, val w: Int, val h: Int)

    private val partitions = listOf(
        Partition(233, 147, 229, 120), Partition(566, 148, 237, 115), Partition(900, 147, 222, 116),
        Partition(236, 283, 229, 124), Partition(564, 280, 237, 120), Partition(900, 282, 222, 120),
        Partition(233, 416, 236, 121), Partition(567, 413, 230, 124), Partition(900, 418, 222, 116)
    )

    private val medalGrid by lazy {
        listOf(I_MEDAL_5, I_MEDAL_4, I_MEDAL_3, I_MEDAL_2, I_MEDAL_1, I_MEDAL_0)
    }

    override suspend fun run() {
        log("=== 个人突破任务开始 ===")
        log("最大挑战次数: ${config.numberAttack}")

        // 处理呱太弹窗
        val img = screenshot()
        if (img != null && I_FROG_RAID.match(img, context).matched) {
            log("检测到呱太活动")
            while (true) {
                val frame = screenshot() ?: break
                if (!I_FROG_RAID.match(frame, context).matched) break
                appearThenClick(I_FROG_RAID, frame)
            }
        }

        // 锁定阵容
        ensureLock(config.lockTeam)

        // 主循环
        var success = true
        var lastBattle = true

        while (true) {
            val frame = screenshot() ?: continue

            // 弹窗处理
            if (I_FRESH_ENSURE.match(frame, context).matched) {
                appearThenClick(I_FRESH_ENSURE, frame)
                continue
            }

            // 挑战次数
            if (currentCount >= config.numberAttack) {
                log("达到挑战次数上限: $currentCount")
                break
            }

            // 寻找勋章
            val (medal, index) = findMedal(frame)
            if (medal == null) {
                log("无可攻击目标，尝试刷新")
                if (checkRefresh()) continue
                success = false; break
            }

            // 退四打九
            if (index == 1 && config.exitFour) {
                log("第一个位置，退四策略")
                repeat(4) { fireAt(index!!); delay(2000) }
            }

            // 挑战
            fireAt(index!!)
            lastBattle = waitBattleEnd()

            // 三胜刷新
            if (config.threeRefresh && I_RR_THREE.match(frame, context).matched) {
                log("三胜刷新")
                if (checkRefresh()) continue
                success = false; break
            }

            // 失败处理：继续
            if (!lastBattle) {
                log("战斗失败，继续")
            }

            currentCount++
        }

        // 退出
        device.click(1178 + 57 / 2, 101 + 64 / 2)
        delay(1000)

        // 设置探索下次运行：当前时间 + 2 分钟
        val nextExplorationTime = System.currentTimeMillis() + 2 * 60 * 1000
        setExplorationNextRun(nextExplorationTime)

        log("=== 个人突破完成，成功=$success，次数=$currentCount，探索2分钟后恢复 ===")
    }

    /**
     * 确保锁定/解锁状态
     */
    private suspend fun ensureLock(lock: Boolean) {
        if (lock) {
            log("锁定阵容")
            repeat(10) {
                val img = screenshot() ?: return
                if (I_LOCK.match(img, context).matched) return
                appearThenClick(I_UNLOCK, img)
            }
        } else {
            log("解锁阵容")
            repeat(10) {
                val img = screenshot() ?: return
                if (I_UNLOCK.match(img, context).matched) return
                appearThenClick(I_LOCK, img)
            }
        }
    }

    /**
     * 寻找可攻击的勋章
     */
    private suspend fun findMedal(img: android.graphics.Bitmap): Pair<RuleImage?, Int?> {
        for (medal in medalGrid) {
            val result = medal.match(img, context)
            if (result.matched) {
                // 判断属于哪个分区
                for ((i, p) in partitions.withIndex()) {
                    if (result.centerX in p.x until (p.x + p.w) &&
                        result.centerY in p.y until (p.y + p.h)) {
                        log("找到勋章 [${medal.name}]，位置 ${i + 1}")
                        return Pair(medal, i + 1)
                    }
                }
            }
        }
        return Pair(null, null)
    }

    /**
     * 点击挑战
     */
    private suspend fun fireAt(position: Int) {
        val p = partitions[position - 1]
        device.click(p.x + p.w / 2, p.y + p.h / 2)
        delay(2000)
        // 等待挑战按钮出现
        repeat(20) {
            val img = screenshot() ?: return
            if (!I_RR_PERSON.match(img, context).matched) return
            appearThenClick(I_FIRE, img)
            delay(500)
        }
        log("点击挑战位置 $position")
    }

    /**
     * 等待战斗结束
     */
    private suspend fun waitBattleEnd(): Boolean {
        log("等待战斗结果...")
        // 等待回到突破页面
        repeat(60) {
            delay(1000)
            val img = screenshot() ?: return false
            if (I_BACK_RED.match(img, context).matched) return true
        }
        return false
    }

    /**
     * 刷新列表
     */
    private suspend fun checkRefresh(): Boolean {
        val img = screenshot() ?: return false
        if (!I_FRESH.match(img, context).matched) {
            log("刷新按钮不可用（CD中）")
            return false
        }
        appearThenClick(I_FRESH, img)
        delay(1000)
        // 确认刷新
        repeat(10) {
            val frame = screenshot() ?: return false
            if (I_FRESH_ENSURE.match(frame, context).matched) {
                appearThenClick(I_FRESH_ENSURE, frame)
                return true
            }
        }
        return false
    }
}
