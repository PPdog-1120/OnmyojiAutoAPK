package com.onmyoji.auto.engine

import android.content.Context
import com.onmyoji.auto.model.TaskConfig
import com.onmyoji.auto.model.TaskType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 任务管理器 — 调度和执行任务
 */
class TaskManager(private val context: Context) {

    enum class State { IDLE, RUNNING, STOPPING }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _currentTask = MutableStateFlow<TaskType?>(null)
    val currentTask: StateFlow<TaskType?> = _currentTask

    // 任务日志流 — UI 可以 collect
    private val _taskLogs = MutableSharedFlow<String>(replay = 200)
    val taskLogs: SharedFlow<String> = _taskLogs.asSharedFlow()

    // 日志文件路径
    private val logDir: File
        get() {
            // 优先用外部存储（方便adb pull），回退到内部存储
            val dir = context.getExternalFilesDir(null)?.let {
                File(it, "logs")
            } ?: File(context.filesDir, "logs")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private var logWriter: FileWriter? = null
    private var currentLogFile: File? = null

    /** 获取最近一次日志文件路径（UI 展示用） */
    var lastLogFilePath: String? = null
        private set

    private var job: Job? = null
    private var deviceController: DeviceController? = null

    fun setDeviceController(controller: DeviceController) {
        deviceController = controller
    }

    fun startTask(type: TaskType, config: TaskConfig) {
        if (_state.value == State.RUNNING) return
        job = CoroutineScope(Dispatchers.Default).launch {
            _state.value = State.RUNNING
            _currentTask.value = type

            // 打开日志文件
            openLogWriter(type)

            try {
                val controller = deviceController
                if (controller == null) {
                    emitAndLog("[错误] DeviceController 为空，无障碍服务可能未连接")
                    return@launch
                }
                val task = when (type) {
                    TaskType.EXPLORATION -> ExplorationTask(context, controller, config)
                    TaskType.REALM_RAID -> RealmRaidTask(context, controller, config)
                }
                val logJob = launch {
                    task.logs.collect { line ->
                        emitAndLog(line)
                    }
                }
                task.run()
                logJob.cancel()
            } catch (e: Exception) {
                android.util.Log.e("TaskManager", "Task error", e)
                emitAndLog("[异常] ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                emitAndLog("=== 任务结束 ===")
                closeLogWriter()
                _state.value = State.IDLE
                _currentTask.value = null
            }
        }
    }

    fun stopTask() {
        _state.value = State.STOPPING
        job?.cancel()
        job = null
        closeLogWriter()
        _state.value = State.IDLE
        _currentTask.value = null
    }

    fun isRunning(): Boolean = _state.value == State.RUNNING

    // ========== 日志文件操作 ==========

    private fun openLogWriter(type: TaskType) {
        try {
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val taskName = if (type == TaskType.EXPLORATION) "exploration" else "realm_raid"
            val file = File(logDir, "${taskName}_${ts}.log")
            currentLogFile = file
            lastLogFilePath = file.absolutePath
            logWriter = FileWriter(file, true)
            // 写入文件头
            val header = buildString {
                appendLine("=== OnmyojiAuto 日志 ===")
                appendLine("任务: ${if (type == TaskType.EXPLORATION) "探索" else "个人突破"}")
                appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("设备: ${android.os.Build.MODEL} (${android.os.Build.DISPLAY})")
                appendLine("屏幕: ${context.resources.displayMetrics.widthPixels}x${context.resources.displayMetrics.heightPixels}")
                appendLine("========================")
            }
            logWriter?.write(header)
            logWriter?.flush()
        } catch (e: Exception) {
            android.util.Log.e("TaskManager", "Failed to open log file", e)
        }
    }

    private fun emitAndLog(line: String) {
        _taskLogs.tryEmit(line)
        try {
            logWriter?.write(line)
            logWriter?.write("\n")
            logWriter?.flush()
        } catch (_: Exception) {}
    }

    private fun closeLogWriter() {
        try {
            logWriter?.flush()
            logWriter?.close()
        } catch (_: Exception) {}
        logWriter = null
    }
}
