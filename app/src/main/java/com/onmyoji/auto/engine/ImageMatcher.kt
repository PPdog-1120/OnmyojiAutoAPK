package com.onmyoji.auto.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.IOException
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 图像匹配引擎 — 纯 Java 实现模板匹配（无需 OpenCV）
 *
 * 使用归一化互相关 (NCC) 算法进行模板匹配
 */
object ImageMatcher {

    /**
     * 模板匹配：在 source 中查找 template
     */
    fun match(
        source: Bitmap,
        template: Bitmap,
        roi: IntArray? = null,  // [x, y, w, h]
        threshold: Float = 0.8f
    ): MatchResult {
        // 裁剪搜索区域
        val src = if (roi != null) {
            val x = roi[0].coerceIn(0, source.width - 1)
            val y = roi[1].coerceIn(0, source.height - 1)
            val w = roi[2].coerceAtMost(source.width - x)
            val h = roi[3].coerceAtMost(source.height - y)
            Bitmap.createBitmap(source, x, y, w, h)
        } else source

        val tw = template.width
        val th = template.height
        if (tw > src.width || th > src.height) return MatchResult(false)

        // 转为灰度像素数组
        val srcGray = bitmapToGray(src)
        val tplGray = bitmapToGray(template)
        val sw = src.width
        val sh = src.height

        // 模板均值和标准差
        val tplMean = tplGray.average()
        val tplStd = stdDev(tplGray, tplMean)
        if (tplStd < 1.0) return MatchResult(false)

        var bestScore = 0f
        var bestX = 0
        var bestY = 0

        // 滑动窗口 NCC（步长2加速，精确定位在最后）
        val step = if (sw * sh > 500000) 3 else 2
        for (y in 0..(sh - th) step step) {
            for (x in 0..(sw - tw) step step) {
                val score = calcNCC(srcGray, sw, x, y, tw, th, tplGray, tplMean, tplStd)
                if (score > bestScore) {
                    bestScore = score
                    bestX = x
                    bestY = y
                }
            }
        }

        // 精确定位
        if (step > 1) {
            val searchR = 3
            for (dy in -searchR..searchR) {
                for (dx in -searchR..searchR) {
                    val px = (bestX + dx).coerceIn(0, sw - tw)
                    val py = (bestY + dy).coerceIn(0, sh - th)
                    val score = calcNCC(srcGray, sw, px, py, tw, th, tplGray, tplMean, tplStd)
                    if (score > bestScore) {
                        bestScore = score
                        bestX = px
                        bestY = py
                    }
                }
            }
        }

        val offsetX = roi?.get(0) ?: 0
        val offsetY = roi?.get(1) ?: 0

        return MatchResult(
            matched = bestScore >= threshold,
            confidence = bestScore,
            x = bestX + offsetX,
            y = bestY + offsetY,
            width = tw,
            height = th
        )
    }

    /**
     * 匹配所有出现位置
     */
    fun matchAll(
        source: Bitmap,
        template: Bitmap,
        roi: IntArray? = null,
        threshold: Float = 0.8f
    ): List<MatchResult> {
        val results = mutableListOf<MatchResult>()
        var currentSource = source.copy(source.config, true)

        repeat(20) {
            val result = match(currentSource, template, roi, threshold)
            if (!result.matched) return@repeat
            results.add(result)

            // 抑制已找到区域
            val g = result.graphics()
            if (g != null) {
                val canvas = android.graphics.Canvas(currentSource)
                val paint = android.graphics.Paint().apply {
                    color = Color.BLACK
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawRect(
                    result.x.toFloat(), result.y.toFloat(),
                    (result.x + result.width).toFloat(),
                    (result.y + result.height).toFloat(),
                    paint
                )
            }
        }
        return results
    }

    /**
     * 计算归一化互相关系数
     */
    private fun calcNCC(
        srcGray: IntArray, srcWidth: Int,
        ox: Int, oy: Int, tw: Int, th: Int,
        tplGray: IntArray, tplMean: Double, tplStd: Double
    ): Float {
        var sum = 0.0
        var sumSq = 0.0
        val n = tw * th

        for (ty in 0 until th) {
            for (tx in 0 until tw) {
                val sv = srcGray[(oy + ty) * srcWidth + (ox + tx)].toDouble()
                sum += sv
                sumSq += sv * sv
            }
        }

        val srcMean = sum / n
        val srcStd = sqrt(sumSq / n - srcMean * srcMean)
        if (srcStd < 1.0) return 0f

        // 互相关
        var crossCorr = 0.0
        for (ty in 0 until th) {
            for (tx in 0 until tw) {
                val sv = srcGray[(oy + ty) * srcWidth + (ox + tx)].toDouble()
                val tv = tplGray[ty * tw + tx].toDouble()
                crossCorr += (sv - srcMean) * (tv - tplMean)
            }
        }

        val denom = srcStd * tplStd * n
        return if (denom > 0) (crossCorr / denom).toFloat() else 0f
    }

    private fun bitmapToGray(bitmap: Bitmap): IntArray {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return IntArray(pixels.size) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        }
    }

    private fun stdDev(data: IntArray, mean: Double): Double {
        var sum = 0.0
        for (v in data) {
            val d = v.toDouble() - mean
            sum += d * d
        }
        return sqrt(sum / data.size)
    }

    /**
     * 加载 assets 中的图片
     */
    fun loadAsset(context: Context, path: String): Bitmap? {
        return try {
            BitmapFactory.decodeStream(context.assets.open(path))
        } catch (e: IOException) {
            android.util.Log.w("ImageMatcher", "Failed to load: $path")
            null
        }
    }
}

data class MatchResult(
    val matched: Boolean,
    val confidence: Float = 0f,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 0,
    val height: Int = 0
) {
    val centerX get() = x + width / 2
    val centerY get() = y + height / 2

    fun graphics(): Any? = null
}
