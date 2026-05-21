package com.onmyoji.auto.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.onmyoji.auto.engine.DeviceController

/**
 * 无障碍服务 — 核心自动化入口
 *
 * 提供：
 * - 手势分发（点击/滑动）
 * - 屏幕内容读取
 * - 节点查找
 */
class AutomationService : AccessibilityService() {

    companion object {
        var instance: AutomationService? = null
            private set
    }

    var deviceController: DeviceController? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        deviceController = DeviceController(this)
        android.util.Log.i("AutomationService", "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 事件驱动由任务引擎处理
    }

    override fun onInterrupt() {
        android.util.Log.w("AutomationService", "无障碍服务被中断")
    }

    override fun onDestroy() {
        instance = null
        deviceController = null
        super.onDestroy()
        android.util.Log.i("AutomationService", "无障碍服务已销毁")
    }

    /**
     * 执行手势（从外部调用）
     */
    fun performClick(x: Int, y: Int) {
        val controller = deviceController ?: return
        // 使用协程在主线程执行
        android.os.Handler(mainLooper).post {
            // 手势通过 DeviceController 的协程方法执行
        }
    }
}
