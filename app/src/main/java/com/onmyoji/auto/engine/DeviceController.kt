package com.onmyoji.auto.engine

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import com.onmyoji.auto.service.ScreenCaptureService
import kotlinx.coroutines.delay

/**
 * 设备控制器 — 通过无障碍服务实现点击/滑动/截图
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
     * 截图 — 从 ScreenCaptureService 获取最新截图
     */
    fun takeScreenshot(): Bitmap? {
        return ScreenCaptureService.latestBitmap ?: lastScreenshot
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
