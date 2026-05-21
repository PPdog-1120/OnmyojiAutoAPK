package com.onmyoji.auto.service
import android.content.pm.ServiceInfo
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.onmyoji.auto.R

class ScreenCaptureService : Service() {

    companion object {
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 1001

        var instance: ScreenCaptureService? = null
            private set

        var latestBitmap: Bitmap? = null
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 1280
    private var screenHeight = 720
    private var screenDensity = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("resultData", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("resultData")
        }
        if (code != Activity.RESULT_OK || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // 先启动前台服务（此时 MediaProjection 权限已通过 Intent 传入）
        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        startCapture(code, data)
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, resultData: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

        // Android 14+ 必须在 createVirtualDisplay 之前注册回调
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                virtualDisplay?.release()
                imageReader?.close()
            }
        }, android.os.Handler(mainLooper))

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "OnmyojiCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                val bitmap = Bitmap.createBitmap(
                    screenWidth + rowPadding / pixelStride,
                    screenHeight,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                latestBitmap = if (bitmap.width != screenWidth) {
                    Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
                } else {
                    bitmap
                }
            } finally {
                image.close()
            }
        }, null)
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        instance = null
        latestBitmap = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "屏幕录制",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "屏幕截图服务"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("阴阳师自动")
            .setContentText("屏幕截图服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }
}
