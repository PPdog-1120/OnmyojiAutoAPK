package com.onmyoji.auto.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import com.onmyoji.auto.service.ScreenCaptureService
import kotlinx.coroutines.delay

/**
 * 设备控制器 — 通过无障碍服务实现点击/滑动/截图
 *
 * 分辨率适配：将截图缩放到 1280x720 后再做模板匹配
 */
class DeviceController(private val service: AccessibilityService) {

    companion object {
        const val SCREEN_WIDTH = 1280
        const val SCREEN_HEIGHT = 720
    }

    private var lastScreenshot: Bitmap? = null

    /**
     * 点击屏幕坐标
     */
    suspend fun click(x: Int, y: Int, duration: Long = 50) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        service.dispatchGesture(gesture, null, null)
        delay(100)
    }

    /**
     * 长按
     */
    suspend fun longClick(x: Int, y: Int, durationMs: Long = 1500) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
        delay(durationMs + 100)
    }

    /**
     * 滑动
     */
    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 500) {
        val path = Path().apply {
            moveTo(x1.toFloat(), y1.toFloat())
            lineTo(x2.toFloat(), y2.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        service.dispatchGesture(gesture, null, null)
        delay(durationMs + 200)
    }

    /**
     * 截图 — 从 ScreenCaptureService 获取最新截图，并缩放到标准分辨率
     */
    fun takeScreenshot(): Bitmap? {
        val raw = ScreenCaptureService.latestBitmap ?: lastScreenshot ?: return null
        return scaleToStandard(raw)
    }

    /**
     * 将截图缩放到标准分辨率 (1280x720)
     *
     * 使用 Canvas 绘制转换，确保任何 Bitmap 格式（包括 HARDWARE）都能正确读取像素
     *
     * 流程：
     * 1. 用 Canvas 将原始 Bitmap 绘制到新的 ARGB_8888 Bitmap（确保可读）
     * 2. 如果是竖屏，先旋转为横屏
     * 3. 等比缩放 + 居中裁剪到 1280x720
     */
    private fun scaleToStandard(source: Bitmap): Bitmap {
        // 第一步：用 Canvas 绘制，确保像素可读（处理 HARDWARE 等特殊格式）
        val readable = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(readable).drawBitmap(source, 0f, 0f, null)

        // 第二步：竖屏旋转
        var processed: Bitmap = readable
        if (readable.width < readable.height) {
            val matrix = Matrix().apply { postRotate(90f) }
            processed = Bitmap.createBitmap(readable, 0, 0, readable.width, readable.height, matrix, true)
            if (processed !== readable) readable.recycle()
        }

        val pw = processed.width
        val ph = processed.height

        // 已经是标准分辨率
        if (pw == SCREEN_WIDTH && ph == SCREEN_HEIGHT) return processed

        // 第三步：等比缩放
        val scale = maxOf(SCREEN_WIDTH.toFloat() / pw, SCREEN_HEIGHT.toFloat() / ph)
        val scaledW = (pw * scale).toInt()
        val scaledH = (ph * scale).toInt()
        val scaleMatrix = Matrix().apply { setScale(scale, scale) }
        val scaled = Bitmap.createBitmap(processed, 0, 0, pw, ph, scaleMatrix, true)

        // 第四步：居中裁剪
        val cropX = ((scaledW - SCREEN_WIDTH) / 2).coerceAtLeast(0)
        val cropY = ((scaledH - SCREEN_HEIGHT) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, SCREEN_WIDTH, SCREEN_HEIGHT)

        // 回收中间产物
        if (scaled !== processed && scaled !== cropped) scaled.recycle()
        if (processed !== source && processed !== readable) processed.recycle()

        return cropped
    }

    fun updateScreenshot(bitmap: Bitmap) {
        lastScreenshot = bitmap
    }

    /**
     * 查找节点
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return findNodeRecursive(root, text)
    }

    private fun findNodeRecursive(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeRecursive(child, text)
            if (found != null) return found
        }
        return null
    }
}
