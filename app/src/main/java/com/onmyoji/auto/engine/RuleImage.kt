package com.onmyoji.auto.engine

import android.content.Context
import android.graphics.Bitmap

/**
 * 图片匹配规则 — 对应 OAS 的 RuleImage
 */
data class RuleImage(
    val name: String,
    val assetPath: String,
    val roiFront: IntArray, // [x, y, w, h]
    val roiBack: IntArray, // [x, y, w, h] 搜索区域
    val threshold: Float = 0.8f
) {
    private var cachedTemplate: Bitmap? = null

    fun getTemplate(context: Context): Bitmap? {
        if (cachedTemplate == null) {
            cachedTemplate = ImageMatcher.loadAsset(context, assetPath)
        }
        return cachedTemplate
    }

    fun match(source: Bitmap, context: Context): MatchResult {
        val template = getTemplate(context) ?: return MatchResult(false)
        val roi = intArrayOf(roiBack[0], roiBack[1], roiBack[2], roiBack[3])
        return ImageMatcher.match(source, template, roi, threshold)
    }

    fun matchAll(source: Bitmap, context: Context, roi: IntArray? = null): List<MatchResult> {
        val template = getTemplate(context) ?: return emptyList()
        val searchRoi = roi ?: roiBack
        return ImageMatcher.matchAll(source, template, searchRoi, threshold)
    }

    fun coord(): Pair<Int, Int> {
        return Pair(roiFront[0] + roiFront[2] / 2, roiFront[1] + roiFront[3] / 2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RuleImage) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

/**
 * 点击规则
 */
data class RuleClick(
    val name: String,
    val roiFront: IntArray
) {
    fun coord(): Pair<Int, Int> {
        return Pair(roiFront[0] + roiFront[2] / 2, roiFront[1] + roiFront[3] / 2)
    }
}

/**
 * 滑动规则
 */
data class RuleSwipe(
    val name: String,
    val startX: Int, val startY: Int,
    val endX: Int, val endY: Int
)
