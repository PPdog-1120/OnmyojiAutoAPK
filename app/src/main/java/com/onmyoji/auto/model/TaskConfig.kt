package com.onmyoji.auto.model

/**
 * 任务配置数据类
 */
data class TaskConfig(
    val explorationLevel: String = "第二十八章",
    val limitTimeMinutes: Int = 30,
    val minionsCount: Int = 30,
    val autoRotate: Boolean = true,
    val chooseRarity: String = "N卡",

    // 个人突破
    val numberAttack: Int = 30,
    val exitFour: Boolean = true,
    val orderAttack: String = "5>4>3>2>1>0",
    val threeRefresh: Boolean = false,

    // 通用战斗
    val lockTeam: Boolean = false
)

enum class TaskType { EXPLORATION, REALM_RAID }
