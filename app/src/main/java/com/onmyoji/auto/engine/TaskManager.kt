package com.onmyoji.auto.engine

import android.content.Context
import com.onmyoji.auto.model.TaskConfig
import com.onmyoji.auto.model.TaskType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 任务管理器 — 调度和执行任务
 */
class TaskManager(private val context: Context) {

    enum class State { IDLE, RUNNING, STOPPING }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _currentTask = MutableStateFlow<TaskType?>(null)
    val currentTask: StateFlow<TaskType?> = _currentTask

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
            try {
                val controller = deviceController ?: return@launch
                val task = when (type) {
                    TaskType.EXPLORATION -> ExplorationTask(context, controller, config)
                    TaskType.REALM_RAID -> RealmRaidTask(context, controller, config)
                }
                task.run()
            } catch (e: Exception) {
                android.util.Log.e("TaskManager", "Task error", e)
            } finally {
                _state.value = State.IDLE
                _currentTask.value = null
            }
        }
    }

    fun stopTask() {
        _state.value = State.STOPPING
        job?.cancel()
        job = null
        _state.value = State.IDLE
        _currentTask.value = null
    }

    fun isRunning(): Boolean = _state.value == State.RUNNING
}
