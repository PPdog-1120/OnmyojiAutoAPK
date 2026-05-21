package com.onmyoji.auto.model

/**
 * 任务配置数据类
 */
data class TaskConfig(
    val explorationLevel: String = "第二十八章",
    val userStatus: UserStatus = UserStatus.ALONE,
    val limitTimeMinutes: Int = 30,
    val minionsCount: Int = 30,
    val autoRotate: Boolean = false,
    val chooseRarity: String = "N卡",
    val upType: UpType = UpType.ALL,
    val buffGold50: Boolean = false,
    val buffExp50: Boolean = false,

    // 个人突破
    val numberAttack: Int = 30,
    val exitFour: Boolean = true,
    val orderAttack: String = "5>4>3>2>1>0",
    val threeRefresh: Boolean = false,
    val whenAttackFail: AttackFailStrategy = AttackFailStrategy.REFRESH,

    // 通用战斗
    val lockTeam: Boolean = false
)

enum class UserStatus { ALONE, LEADER, MEMBER }
enum class UpType { ALL, EXP, COIN, DARUMA }
enum class AttackFailStrategy { EXIT, CONTINUE, REFRESH }
enum class TaskType { EXPLORATION, REALM_RAID }
