package com.onmyoji.auto.engine
import kotlinx.coroutines.delay
import android.content.Context
import com.onmyoji.auto.model.TaskConfig
import com.onmyoji.auto.model.UpType
import com.onmyoji.auto.model.UserStatus

/**
 * 探索任务 — 保留 OAS 核心逻辑
 *
 * 流程：场景识别 → 章节选择 → 战斗循环（UP怪优先/小怪/Boss） → 退出
 */
class ExplorationTask(
    context: Context,
    device: DeviceController,
    config: TaskConfig
) : BaseTask(context, device, config) {

    // ========== 资源定义 ==========
    // 场景识别
    private val I_CHECK_EXPLORATION = RuleImage("check_exploration",
        "exploration/res_exploration_title.png",
        intArrayOf(1133, 124, 47, 43), intArrayOf(1100, 100, 180, 100), 0.7f)

    // 探索按钮
    private val I_EXPLORATION_CLICK = RuleImage("exploration_click",
        "exploration/res_e_exploration_click.png",
        intArrayOf(1076, 601, 96, 42), intArrayOf(939, 555, 307, 127), 0.8f)

    // 设置按钮
    private val I_SETTINGS_BUTTON = RuleImage("settings_button",
        "exploration/res_e_settings_button.png",
        intArrayOf(37, 692, 53, 26), intArrayOf(37, 692, 53, 26), 0.65f)

    // 自动轮换
    private val I_AUTO_ROTATE_ON = RuleImage("auto_rotate_on",
        "exploration/res_e_auto_rotate_on.png",
        intArrayOf(104, 649, 153, 44), intArrayOf(104, 649, 153, 44), 0.9f)

    private val I_AUTO_ROTATE_OFF = RuleImage("auto_rotate_off",
        "exploration/res_e_auto_rotate_off.png",
        intArrayOf(108, 650, 150, 46), intArrayOf(108, 650, 150, 46), 0.85f)

    // 普通怪
    private val I_NORMAL_BATTLE = RuleImage("normal_battle",
        "exploration/res_normal_battle_button.png",
        intArrayOf(636, 263, 42, 39), intArrayOf(0, 0, 1279, 719), 0.8f)

    // Boss
    private val I_BOSS_BATTLE = RuleImage("boss_battle",
        "exploration/res_boss_battle_button.png",
        intArrayOf(683, 256, 38, 34), intArrayOf(0, 0, 1276, 719), 0.8f)

    // 战后奖励
    private val I_BATTLE_REWARD = RuleImage("battle_reward",
        "exploration/res_battle_reward.png",
        intArrayOf(647, 395, 31, 21), intArrayOf(1, 1, 1278, 718), 0.9f)

    // UP怪标识
    private val I_UP_EXP = RuleImage("up_exp",
        "exploration/highlight_up_exp.png",
        intArrayOf(471, 518, 74, 71), intArrayOf(1, 225, 1278, 410), 0.8f)

    private val I_UP_COIN = RuleImage("up_coin",
        "exploration/highlight_up_coin.png",
        intArrayOf(330, 529, 74, 74), intArrayOf(1, 317, 1278, 316), 0.8f)

    private val I_UP_DARUMA = RuleImage("up_daruma",
        "exploration/highlight_up_daruma.png",
        intArrayOf(1146, 510, 80, 80), intArrayOf(1, 265, 1278, 369), 0.8f)

    // 宝箱
    private val I_TREASURE_BOX = RuleImage("treasure_box",
        "exploration/res_treasure_box_click.png",
        intArrayOf(33, 476, 70, 49), intArrayOf(2, 130, 135, 406), 0.7f)

    // 滑动结束标识
    private val I_SWIPE_END = RuleImage("swipe_end",
        "exploration/res_swipe_end.png",
        intArrayOf(994, 234, 119, 100), intArrayOf(968, 196, 311, 165), 0.8f)

    // 退出确认
    private val I_EXIT_CONFIRM = RuleImage("exit_confirm",
        "exploration/res_e_exit_confirm.png",
        intArrayOf(694, 380, 163, 49), intArrayOf(694, 380, 163, 49), 0.8f)

    // 红色关闭
    private val I_RED_CLOSE = RuleImage("red_close",
        "exploration/res_red_close.png",
        intArrayOf(1027, 129, 41, 42), intArrayOf(1021, 121, 54, 55), 0.6f)

    // 队伍表情
    private val I_TEAM_EMOJI = RuleImage("team_emoji",
        "exploration/res_team_emoji.png",
        intArrayOf(36, 437, 44, 46), intArrayOf(4, 407, 100, 100), 0.8f)

    // 箭头
    private val I_ARROW_LEFT = RuleImage("arrow_left",
        "exploration/res_exp_arrow_left.png",
        intArrayOf(1244, 115, 18, 26), intArrayOf(1178, 78, 100, 100), 0.8f)

    private val I_ARROW_RIGHT = RuleImage("arrow_right",
        "exploration/res_exp_arrow_right.png",
        intArrayOf(1240, 117, 24, 21), intArrayOf(1178, 74, 100, 100), 0.8f)

    private var minionsCnt = 0
    private var searchFailCnt = 0

    override suspend fun run() {
        log("=== 探索任务开始 ===")
        log("章节: ${config.explorationLevel}")
        log("模式: ${config.userStatus}")

        // 导航到探索页面
        navigateToExploration()

        // 根据模式执行
        when (config.userStatus) {
            UserStatus.ALONE -> runSolo()
            UserStatus.LEADER -> runLeader()
            UserStatus.MEMBER -> runMember()
        }

        log("=== 探索完成，战斗 $minionsCnt 次 ===")
    }

    private suspend fun navigateToExploration() {
        log("导航到探索页面...")
        // 实际实现需要 GameUi 页面导航
        // 这里简化为直接点击探索入口
        waitUntilAppear(I_CHECK_EXPLORATION, 15000)
    }

    /**
     * 单人模式
     */
    private suspend fun runSolo() {
        log("单人模式启动")
        searchFailCnt = 0

        while (true) {
            val img = screenshot() ?: continue
            val scene = detectScene(img)

            when (scene) {
                Scene.WORLD -> {
                    // 展开箭头
                    appearThenClick(I_ARROW_LEFT, img)
                    // 宝箱
                    if (I_TREASURE_BOX.match(img, context).matched) {
                        log("发现宝箱")
                        appearThenClick(I_TREASURE_BOX, img)
                    }
                    if (checkExit()) return
                    // 选择章节
                    selectChapter()
                }
                Scene.MAIN -> {
                    // 战后奖励
                    if (I_BATTLE_REWARD.match(img, context).matched) {
                        appearThenClick(I_BATTLE_REWARD, img)
                        continue
                    }
                    // Boss
                    if (I_BOSS_BATTLE.match(img, context).matched) {
                        if (fire(I_BOSS_BATTLE, img)) {
                            log("Boss战斗，第 ${minionsCnt} 次")
                        }
                        continue
                    }
                    // 小怪（UP优先）
                    val fightBtn = findUpFight(img)
                    if (fightBtn != null) {
                        if (fire(fightBtn, img)) {
                            log("战斗，第 ${minionsCnt} 次")
                        }
                        continue
                    }
                    // 滑动寻找
                    if (searchFailCnt >= 4) {
                        searchFailCnt = 0
                        if (I_SWIPE_END.match(img, context).matched) {
                            quitExplore()
                            return
                        }
                        device.swipe(1093, 148, 397, 140, 350)
                        delay(2000)
                    } else {
                        searchFailCnt++
                    }
                }
                Scene.BATTLE -> {
                    // 等待战斗结束
                    log("等待战斗结束...")
                    delay(3000)
                }
                else -> delay(500)
            }
        }
    }

    /**
     * 队长模式
     */
    private suspend fun runLeader() {
        log("队长模式启动")
        // 类似 solo，但包含组队和邀请逻辑
        runSolo() // 简化处理
    }

    /**
     * 队员模式
     */
    private suspend fun runMember() {
        log("队员模式启动")
        // 队员只需要战斗，不需要导航
    }

    /**
     * 场景识别
     */
    private fun detectScene(img: android.graphics.Bitmap): Scene {
        if (I_CHECK_EXPLORATION.match(img, context).matched &&
            !I_SETTINGS_BUTTON.match(img, context).matched) {
            return Scene.WORLD
        }
        if (I_EXPLORATION_CLICK.match(img, context).matched) return Scene.ENTRANCE
        if (I_SETTINGS_BUTTON.match(img, context).matched ||
            I_AUTO_ROTATE_ON.match(img, context).matched ||
            I_AUTO_ROTATE_OFF.match(img, context).matched) {
            return Scene.MAIN
        }
        return Scene.UNKNOWN
    }

    /**
     * 选择章节
     */
    private suspend fun selectChapter() {
        log("选择章节: ${config.explorationLevel}")
        // OCR识别章节列表，滑动到目标章节
        // 简化实现：直接点击探索按钮
        appearThenClick(I_EXPLORATION_CLICK, interval = 2000)
    }

    /**
     * 寻找UP怪
     */
    private fun findUpFight(img: android.graphics.Bitmap): RuleImage? {
        val upRule = when (config.upType) {
            UpType.EXP -> I_UP_EXP
            UpType.COIN -> I_UP_COIN
            UpType.DARUMA -> I_UP_DARUMA
            UpType.ALL -> {
                return if (I_NORMAL_BATTLE.match(img, context).matched) I_NORMAL_BATTLE else null
            }
        }

        val upResult = upRule.match(img, context)
        if (!upResult.matched) return null

        // 在UP图标附近找战斗按钮
        val roi = intArrayOf(
            (upResult.x - 60).coerceAtLeast(0),
            (upResult.y - 300).coerceAtLeast(0),
            120,
            280
        )
        val matches = I_NORMAL_BATTLE.matchAll(img, context, roi)
        if (matches.isEmpty()) return null

        // 按距离排序
        val best = matches.minByOrNull {
            val dx = (it.centerX - upResult.centerX) * 3
            val dy = it.centerY - upResult.centerY
            dx * dx + dy * dy
        }
        return if (best != null) I_NORMAL_BATTLE else null
    }

    /**
     * 发起战斗
     */
    private suspend fun fire(rule: RuleImage, img: android.graphics.Bitmap? = null): Boolean {
        appearThenClick(rule, img)
        delay(3000)
        minionsCnt++
        currentCount = minionsCnt
        return true
    }

    /**
     * 退出探索
     */
    private suspend fun quitExplore() {
        log("退出探索")
        repeat(5) {
            val img = screenshot() ?: return
            if (I_CHECK_EXPLORATION.match(img, context).matched) return
            appearThenClick(I_EXIT_CONFIRM, img)
            appearThenClick(I_RED_CLOSE, img)
            delay(1000)
        }
    }

    private suspend fun checkExit(): Boolean {
        if (minionsCnt >= config.minionsCount) {
            log("战斗次数达标")
            return true
        }
        if (isTimeUp(config.limitTimeMinutes)) {
            log("时间限制到达")
            return true
        }
        return false
    }

    private enum class Scene { WORLD, ENTRANCE, MAIN, BATTLE, UNKNOWN }
}
