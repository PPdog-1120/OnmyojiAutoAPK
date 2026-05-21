package com.onmyoji.auto.engine

import android.content.Context
import android.graphics.Bitmap
import com.onmyoji.auto.model.TaskConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务基类 — 提供通用的截图、匹配、点击、等待能力
 */
abstract class BaseTask(
    protected val context: Context,
    protected val device: DeviceController,
    protected val config: TaskConfig
) {
    protected val assets = context.assets
    var currentCount = 0
        protected set
    protected val startTime = System.currentTimeMillis()

    // 日志流
    private val _logs = MutableSharedFlow<String>(replay = 100)
    val logs: SharedFlow<String> = _logs

    protected suspend fun log(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val line = "[$ts] $msg"
        _logs.emit(line)
        android.util.Log.d("OnmyojiTask", line)
    }

    /**
     * 执行任务
     */
    abstract suspend fun run()

    /**
     * 截图
     */
    protected fun screenshot(): Bitmap? {
        return device.takeScreenshot()
    }

    /**
     * 出现则点击
     */
    protected suspend fun appearThenClick(
        rule: RuleImage,
        screenshot: Bitmap? = null,
        interval: Long = 1000,
        threshold: Float = 0.8f
    ): Boolean {
        val img = screenshot ?: this.screenshot() ?: return false
        val result = rule.match(img, context)
        if (result.matched) {
            device.click(result.centerX, result.centerY)
            delay(interval)
            return true
        }
        return false
    }

    /**
     * 等待目标出现
     */
    protected suspend fun waitUntilAppear(
        rule: RuleImage,
        timeoutMs: Long = 10000,
        intervalMs: Long = 500
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val img = screenshot()
            if (img != null && rule.match(img, context).matched) return true
            delay(intervalMs)
        }
        return false
    }

    /**
     * 等待目标消失
     */
    protected suspend fun waitUntilDisappear(
        rule: RuleImage,
        timeoutMs: Long = 15000
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val img = screenshot() ?: break
            if (!rule.match(img, context).matched) break
            delay(300)
        }
    }

    /**
     * 循环点击直到出现stop
     */
    protected suspend fun uiClickUntil(
        clickRule: RuleImage,
        stopRule: RuleImage,
        intervalMs: Long = 1000,
        timeoutMs: Long = 30000
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val img = screenshot() ?: continue
            if (stopRule.match(img, context).matched) return true
            appearThenClick(clickRule, img, intervalMs)
        }
        return false
    }

    /**
     * 随机延迟（防检测）
     */
    protected suspend fun randomDelay(min: Long = 500, max: Long = 2000) {
        delay((min..max).random())
    }

    /**
     * 检查时间限制
     */
    protected fun isTimeUp(limitMinutes: Int): Boolean {
        return System.currentTimeMillis() - startTime > limitMinutes * 60_000L
    }
}
