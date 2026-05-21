package com.onmyoji.auto.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

/**
 * OCR 区域定义 — 对应 OAS 的 OcrRegion
 * 简化实现：截取区域 → 降噪 → 像素密度分析 → 返回数字文本
 * 如需更高精度，可替换为 ML Kit Text Recognition
 */
data class OcrRegion(
    val name: String,
    val roiFront: IntArray,  // [x, y, w, h]
    val roiBack: IntArray,   // [x, y, w, h] 搜索区域
    val expectedChars: String = "0123456789"
) {
    /**
     * 对指定区域执行 OCR，返回识别到的数字文本
     */
    fun ocr(source: Bitmap, context: Context): String {
        val x = roiBack[0].coerceIn(0, source.width - 1)
        val y = roiBack[1].coerceIn(0, source.height - 1)
        val w = roiBack[2].coerceAtMost(source.width - x)
        val h = roiBack[3].coerceAtMost(source.height - y)
        if (w <= 0 || h <= 0) return ""

        val cropped = Bitmap.createBitmap(source, x, y, w, h)
        val result = recognizeDigits(cropped)
        cropped.recycle()
        return result
    }

    /**
     * 简易数字识别：基于像素密度分割字符
     */
    private fun recognizeDigits(bitmap: Bitmap): String {
        val w = bitmap.width
        val h = bitmap.height
        if (w == 0 || h == 0) return ""

        // 转灰度并二值化
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val binary = BooleanArray(w * h) { i ->
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            gray < 128  // 黑色文字为 true
        }

        // 按列扫描，找字符边界
        val colHasInk = BooleanArray(w) { col ->
            (0 until h).any { row -> binary[row * w + col] }
        }

        val segments = mutableListOf<Pair<Int, Int>>()
        var start = -1
        for (col in 0 until w) {
            if (colHasInk[col] && start == -1) start = col
            if (!colHasInk[col] && start != -1) {
                segments.add(start to col)
                start = -1
            }
        }
        if (start != -1) segments.add(start to w)

        // 对每个字符段做简单的像素计数匹配
        val sb = StringBuilder()
        for ((segStart, segEnd) in segments) {
            val segW = segEnd - segStart
            if (segW < 2) continue  // 太窄，跳过噪点

            var inkCount = 0
            for (row in 0 until h) {
                for (col in segStart until segEnd) {
                    if (binary[row * w + col]) inkCount++
                }
            }
            val density = inkCount.toFloat() / (segW * h)

            // 基于密度粗略猜数字（0-9）
            // 这是简化实现，生产环境建议用 ML Kit 替换
            val digit = when {
                density < 0.15f -> ' '
                density > 0.65f -> '8'
                density > 0.55f -> '0'
                density > 0.45f -> '6'
                density > 0.35f -> '9'
                density > 0.25f -> '3'
                density > 0.18f -> '2'
                else -> '1'
            }
            if (digit != ' ') sb.append(digit)
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OcrRegion) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}
