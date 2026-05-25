package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.MainActivity
import com.example.R
import com.example.data.SpeedStateHolder
import com.example.data.db.AppDatabase
import com.example.data.repository.DataUsageRepository
import com.example.utils.TrafficUtils
import com.example.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpeedMeterService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serviceJob: Job? = null
    private var isScreenOn = true
    private lateinit var repository: DataUsageRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var prefs: PreferencesManager

    companion object {
        private const val NOTIFICATION_ID = 9182
        private const val CHANNEL_ID = "internet_speed_meter_channel"
        private const val CHANNEL_NAME = "Real-time Internet Speed"
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    startTrackingLoop()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    startTrackingLoop()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getDatabase(this)
        repository = DataUsageRepository(db.dataUsageDao)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        prefs = PreferencesManager(this)

        createNotificationChannel()

        // Register screen state receiver to dynamically adjust sampling rate for low thermal/battery profile
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenStateReceiver, filter)
        }

        SpeedStateHolder.updateServiceStatus(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SpeedStateHolder.updateServiceStatus(true)
        
        // Start foreground right away to meet Android system requirements
        val initialNotification = buildNotification(0L, 0L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startTrackingLoop()

        return START_STICKY
    }

    private fun startTrackingLoop() {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            // Warm up values
            var lastTotalRx = TrafficStats.getTotalRxBytes()
            var lastTotalTx = TrafficStats.getTotalTxBytes()
            var lastMobileRx = TrafficStats.getMobileRxBytes()
            var lastMobileTx = TrafficStats.getMobileTxBytes()
            var lastTime = android.os.SystemClock.elapsedRealtime()

            var lastDbSaveTime = lastTime
            var pendingWifiRx = 0L
            var pendingWifiTx = 0L
            var pendingMobileRx = 0L
            var pendingMobileTx = 0L
            
            var currentDateStr = getTodayDateString()
            var todayRecord = repository.getRecordForDate(currentDateStr)

            while (isActive) {
                // Low power optimization: slow polling when screen is off, fast real-time 1s polling when interactive
                val interval = if (isScreenOn) 1000L else 15000L
                delay(interval)

                val currentTotalRx = TrafficStats.getTotalRxBytes()
                val currentTotalTx = TrafficStats.getTotalTxBytes()
                var currentMobileRx = TrafficStats.getMobileRxBytes()
                var currentMobileTx = TrafficStats.getMobileTxBytes()
                val currentTime = android.os.SystemClock.elapsedRealtime()

                // Gracefully handle values unsupported or not initialized (represented as -1 by OS)
                if (currentMobileRx < 0) currentMobileRx = 0
                if (currentMobileTx < 0) currentMobileTx = 0

                val timeDelta = (currentTime - lastTime).coerceAtLeast(1L)
                val seconds = timeDelta / 1000.0

                // If TrafficStats returned 0 or decreased drastically, assume reboot / reset and adjust last values
                if (currentTotalRx < lastTotalRx || lastTotalRx <= 0) {
                    lastTotalRx = currentTotalRx
                    lastTotalTx = currentTotalTx
                    lastMobileRx = currentMobileRx
                    lastMobileTx = currentMobileTx
                    lastTime = currentTime
                    continue
                }

                val deltaTotalRx = currentTotalRx - lastTotalRx
                val deltaTotalTx = currentTotalTx - lastTotalTx
                val deltaMobileRx = if (currentMobileRx >= lastMobileRx) currentMobileRx - lastMobileRx else currentMobileRx
                val deltaMobileTx = if (currentMobileTx >= lastMobileTx) currentMobileTx - lastMobileTx else currentMobileTx

                val downloadRate = (deltaTotalRx / seconds).toLong()
                val uploadRate = (deltaTotalTx / seconds).toLong()

                // Calculate Wi-Fi portions by subtracting cellular from total
                val wifiRxDelta = (deltaTotalRx - deltaMobileRx).coerceAtLeast(0L)
                val wifiTxDelta = (deltaTotalTx - deltaMobileTx).coerceAtLeast(0L)
                val mobileRxDelta = deltaMobileRx.coerceAtLeast(0L)
                val mobileTxDelta = deltaMobileTx.coerceAtLeast(0L)

                pendingWifiRx += wifiRxDelta
                pendingWifiTx += wifiTxDelta
                pendingMobileRx += mobileRxDelta
                pendingMobileTx += mobileTxDelta

                val todayString = getTodayDateString()
                
                // If day changed, force save the pending old values
                if (todayString != currentDateStr) {
                    if (pendingWifiRx > 0 || pendingWifiTx > 0 || pendingMobileRx > 0 || pendingMobileTx > 0) {
                        repository.addUsage(currentDateStr, pendingWifiRx, pendingWifiTx, pendingMobileRx, pendingMobileTx)
                    }
                    currentDateStr = todayString
                    todayRecord = repository.getRecordForDate(currentDateStr)
                    pendingWifiRx = 0L
                    pendingWifiTx = 0L
                    pendingMobileRx = 0L
                    pendingMobileTx = 0L
                    lastDbSaveTime = currentTime
                } else if (currentTime - lastDbSaveTime >= 60000L) {
                    // Flush to db every 60 seconds
                    if (pendingWifiRx > 0 || pendingWifiTx > 0 || pendingMobileRx > 0 || pendingMobileTx > 0) {
                        repository.addUsage(currentDateStr, pendingWifiRx, pendingWifiTx, pendingMobileRx, pendingMobileTx)
                        pendingWifiRx = 0L
                        pendingWifiTx = 0L
                        pendingMobileRx = 0L
                        pendingMobileTx = 0L
                    }
                    todayRecord = repository.getRecordForDate(currentDateStr)
                    lastDbSaveTime = currentTime
                }

                // Calculate current live stats
                val liveWifiRx = (todayRecord?.wifiRx ?: 0L) + pendingWifiRx
                val liveWifiTx = (todayRecord?.wifiTx ?: 0L) + pendingWifiTx
                val liveMobileRx = (todayRecord?.mobileRx ?: 0L) + pendingMobileRx
                val liveMobileTx = (todayRecord?.mobileTx ?: 0L) + pendingMobileTx

                SpeedStateHolder.updateTodayStats(
                    wifiRx = liveWifiRx,
                    wifiTx = liveWifiTx,
                    mobileRx = liveMobileRx,
                    mobileTx = liveMobileTx
                )

                if (isScreenOn) {
                    SpeedStateHolder.updateRates(downloadRate, uploadRate)
                    updateNotification(downloadRate, uploadRate)
                } else {
                    // Set live states to rest to avoid waking up observers needlessly
                    SpeedStateHolder.updateRates(0L, 0L)
                }

                lastTotalRx = currentTotalRx
                lastTotalTx = currentTotalTx
                lastMobileRx = currentMobileRx
                lastMobileTx = currentMobileTx
                lastTime = currentTime
            }
        }
    }

    private fun updateNotification(downloadRate: Long, uploadRate: Long) {
        val notification = buildNotification(downloadRate, uploadRate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createSpeedIcon(downloadRate: Long, useBits: Boolean): IconCompat {
        val (number, unit) = TrafficUtils.getStatusBarSpeedParts(downloadRate, useBits)
        val width = 96
        val height = 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val numberPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
            textSize = 84f // Maximize height to be as big as time
        }

        val unitPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            isFakeBoldText = true
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 1f
            textSize = 60f
        }

        // Measure text wide without scaling
        var numberWidth = numberPaint.measureText(number)
        var unitWidth = unitPaint.measureText(unit)
        val space = 12f

        var totalWidth = numberWidth + space + unitWidth

        // If it's too wide, squash it horizontally so it fits the box
        // This guarantees maximum height (matching clock size) while staying within the square bounds
        val maxAllowedWidth = width.toFloat() - 4f // 2px padding on each side
        if (totalWidth > maxAllowedWidth) {
            val scaleFactor = maxAllowedWidth / totalWidth
            numberPaint.textScaleX = scaleFactor
            unitPaint.textScaleX = scaleFactor
            
            // Re-measure after scaling
            numberWidth = numberPaint.measureText(number)
            unitWidth = unitPaint.measureText(unit)
            totalWidth = numberWidth + space + unitWidth
        }

        val startX = (width - totalWidth) / 2f
        val textBounds = Rect()
        numberPaint.getTextBounds(number, 0, number.length, textBounds)
        
        // Vertically center based on the large number height
        val y = height / 2f + textBounds.height() / 2f

        canvas.drawText(number, startX, y, numberPaint)
        canvas.drawText(unit, startX + numberWidth + space, y, unitPaint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun buildNotification(downloadRate: Long, uploadRate: Long): Notification {
        val useBits = prefs.speedUnit == PreferencesManager.UNIT_BITS
        val dlSpeedStr = TrafficUtils.formatSpeed(downloadRate, useBits)
        val ulSpeedStr = TrafficUtils.formatSpeed(uploadRate, useBits)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val speedIcon = createSpeedIcon(downloadRate, useBits)
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(speedIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            
        if (prefs.isNotificationHidden) {
            builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
            val rv = android.widget.RemoteViews(packageName, R.layout.empty_notification)
            builder.setCustomContentView(rv)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setContentTitle("↓ $dlSpeedStr   ↑ $ulSpeedStr")
            builder.setContentText("SpeedMeter Live Monitor")
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows real-time internet download and upload speed meter."
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        serviceJob?.cancel()
        SpeedStateHolder.updateServiceStatus(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
