package com.muka.amap_location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import kotlin.random.Random


/** AmapLocationPlugin */
public class AmapLocationPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, GeocodeSearch.OnGeocodeSearchListener {

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var pluginBinding: FlutterPlugin.FlutterPluginBinding
    private var eventSink: EventChannel.EventSink? = null
    private lateinit var watchClient: AMapLocationClient
    private var channelId: String = "plugins.muka.com/amap_location_server"
    private var notificationManager: NotificationManager? = null
    private lateinit var geocoderSearch: GeocodeSearch
    private var geocodeSink: Result? = null
    private var locaPos : HashMap<String, Any>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        pluginBinding = flutterPluginBinding
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "plugins.muka.com/amap_location")
        channel.setMethodCallHandler(this);
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "plugins.muka.com/amap_location_event")
        eventChannel.setStreamHandler(this);
        watchClient = AMapLocationClient(flutterPluginBinding.applicationContext)
        geocoderSearch = GeocodeSearch(flutterPluginBinding.applicationContext);
        geocoderSearch.setOnGeocodeSearchListener(this)
        watchClient.setLocationListener {
            if (it != null) {
                if (it.errorCode == 0) {
                    eventSink?.success(Convert.toJson(it))
                } else {
                    eventSink?.error("AmapError", "onLocationChanged Error: ${it.errorInfo}", it.errorInfo)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "plugins.muka.com/amap_location")
            channel.setMethodCallHandler(AmapLocationPlugin())
            val eventChannel = EventChannel(registrar.messenger(), "plugins.muka.com/amap_location_event")
            eventChannel.setStreamHandler(AmapLocationPlugin());
        }
    }

    override fun onRegeocodeSearched(p0: RegeocodeResult?, p1: Int) {
        if (p0 != null && p1 == 1000 && locaPos != null) {
            locaPos!!["geocode"] = Convert.toJson(p0)
            Log.d("22", locaPos.toString())
            geocodeSink?.success(locaPos)
        } else  {
            geocodeSink?.error("AmapError", "onLocationChanged Error: null", null)
        }
        geocodeSink = null
        locaPos = null
    }

    override fun onGeocodeSearched(p0: GeocodeResult?, p1: Int) {
        TODO("Not yet implemented")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "fetch" -> {
                var mode: Any? = call.argument("mode")
                var geocode: Boolean = call.argument<Boolean>("geocode") ?: false
                var locationClient = AMapLocationClient(pluginBinding.applicationContext)
                var locationOption = AMapLocationClientOption()
                locationOption.locationMode = when (mode) {
                    1 -> AMapLocationClientOption.AMapLocationMode.Battery_Saving
                    2 -> AMapLocationClientOption.AMapLocationMode.Device_Sensors
                    else -> AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                }
                locationClient.setLocationOption(locationOption)
                locationClient.setLocationListener {
                    if (it != null) {
                        if (it.errorCode == 0) {
                            if (geocode) {
                                val query = RegeocodeQuery(LatLonPoint(it.latitude, it.longitude), 200F, GeocodeSearch.AMAP)
                                geocoderSearch.getFromLocationAsyn(query)
                                geocodeSink = result
                                locaPos = Convert.toJson(it)
                            } else {
                                result.success(Convert.toJson(it))
                            }
                        } else {
                            result.error("AmapError", "onLocationChanged Error: ${it.errorInfo}", it.errorInfo)
                        }
                    }
                    locationClient.stopLocation()
                }
                locationClient.startLocation()
            }
            "start" -> {
                var mode: Any? = call.argument("mode")
                var time: Int = call.argument<Int>("time") ?: 2000
                var locationOption = AMapLocationClientOption()
                locationOption.locationMode = when (mode) {
                    1 -> AMapLocationClientOption.AMapLocationMode.Battery_Saving
                    2 -> AMapLocationClientOption.AMapLocationMode.Device_Sensors
                    else -> AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                }
                locationOption.interval = time.toLong();
                watchClient.setLocationOption(locationOption)
                watchClient.startLocation()
                result.success(null)
            }
            "stop" -> {
                watchClient.stopLocation()
                result.success(null)
            }
            "enableBackground" -> {
                watchClient.enableBackgroundLocation(2001, buildNotification(call.argument("title")
                        ?: "", call.argument("label") ?: "", call.argument("assetName")
                        ?: "", call.argument<Boolean>("vibrate")!!))
                result.success(null)
            }
            "disableBackground" -> {
                watchClient.disableBackgroundLocation(true)
                notificationManager?.deleteNotificationChannel(channelId)
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun buildNotification(title: String, label: String, name: String, vibrate: Boolean): Notification? {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel = NotificationChannel(channelId, pluginBinding.applicationContext.packageName, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true) //是否在桌面icon右上角展示小圆点
            notificationChannel.lightColor = Color.BLUE //小圆点颜色
            notificationChannel.setShowBadge(true) //是否在久按桌面图标时显示此渠道的通知

            if (!vibrate) {
                notificationChannel.enableVibration(false)
                notificationChannel.vibrationPattern = null
                notificationChannel.setSound(null, null)
            }
            notificationManager = pluginBinding.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(notificationChannel);
        }
        var intent = Intent(pluginBinding.applicationContext, getMainActivityClass(pluginBinding.applicationContext))
        var pendingIntent = PendingIntent.getActivity(pluginBinding.applicationContext, Random.nextInt(100), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        var notification = NotificationCompat.Builder(pluginBinding.applicationContext, channelId).setContentTitle(title).setContentText(label).setWhen(System.currentTimeMillis()).setSmallIcon(getDrawableResourceId(name)).setLargeIcon(BitmapFactory.decodeResource(pluginBinding.applicationContext.resources, getDrawableResourceId(name))).setContentIntent(pendingIntent)
        if (!vibrate) {
            notification.setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
        }
        return notification.build()
    }

    private fun getMainActivityClass(context: Context): Class<*>? {
        val packageName = context.packageName
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        val className = launchIntent.component.className
        return try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    private fun getDrawableResourceId(name: String): Int {
        return pluginBinding.applicationContext.resources.getIdentifier(name, "drawable", pluginBinding.applicationContext.packageName)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {

    }
}