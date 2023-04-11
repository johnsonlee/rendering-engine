package io.johnsonlee.rendering

import android.util.Log
import com.android.layoutlib.bridge.Bridge
import com.didiglobal.booster.build.AndroidSdk
import com.didiglobal.booster.kotlinx.file
import com.google.android.collect.Maps
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileInputStream
import java.lang.System.currentTimeMillis
import java.util.Properties
import kotlin.concurrent.withLock

private const val ANDROID_30 = "android-30"

private const val TAG = "BridgeLoader"

object BridgeLoader {

    private val sdk by lazy {
        AndroidSdk.location
    }

    private val platform by lazy {
        sdk.file("platforms", ANDROID_30)
    }

    private val data by lazy {
        File(System.getProperty("user.dir"), "data")
    }

    val bridge: Bridge by lazy {
        val systemProperties = Properties().apply {
            platform.file("build.prop").inputStream().use(::load)
        }.asSequence().filterIsInstance<Map.Entry<String, String>>().associate {
            it.key to it.value
        }
        val fontLocation = data.file("fonts")
        val nativeLibLocation = data.file(getNativeLibDir())
        val icuLocation = data.file("icu", "icudt70l.dat")
        val enumMap = getEnumMap(platform.file("data", "res", "values", "attrs.xml"))

        Bridge().apply {
            val t0 = currentTimeMillis()
            check(Bridge.getLock().withLock {
                init(systemProperties, fontLocation, nativeLibLocation.path, icuLocation.path, enumMap, Bridge.getLog())
            }) {
                "Failed to initialize Bridge"
            }
            Bridge.getLog().logAndroidFramework(Log.INFO, TAG, "Initializing Bridge complete in ${currentTimeMillis() - t0}ms")
        }
    }

}

private fun getNativeLibDir(): String {
    val osName = System.getProperty("os.name").lowercase()
    val osLabel = when {
        osName.startsWith("windows") -> "win"
        osName.startsWith("mac") -> {
            val osArch = System.getProperty("os.arch").lowercase()
            if (osArch.startsWith("x86")) "mac" else "mac-arm"
        }

        else -> "linux"
    }
    return "${osLabel}${File.separator}lib64"
}

private const val TAG_ATTR = "attr"
private const val TAG_ENUM = "enum"
private const val TAG_FLAG = "flag"
private const val ATTR_NAME = "name"
private const val ATTR_VALUE = "value"

fun getEnumMap(path: File): Map<String, Map<String, Int>> {
    val map = mutableMapOf<String, MutableMap<String, Int>>()
    val xmlPullParser = XmlPullParserFactory.newInstance().newPullParser().apply {
        setInput(FileInputStream(path), null)
    }
    var eventType = xmlPullParser.eventType
    var attr: String? = null

    while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
            XmlPullParser.START_TAG -> {
                when (xmlPullParser.name) {
                    TAG_ATTR -> {
                        attr = xmlPullParser.getAttributeValue(null, ATTR_NAME)
                    }

                    TAG_ENUM, TAG_FLAG -> {
                        require(attr != null)
                        val name = xmlPullParser.getAttributeValue(null, ATTR_NAME)
                        val value = xmlPullParser.getAttributeValue(null, ATTR_VALUE)
                        val i = (java.lang.Long.decode(value) as Long).toInt()
                        val attributeMap = map.getOrPut(attr, Maps::newHashMap)
                        attributeMap[name] = i
                    }
                }
            }

            XmlPullParser.END_TAG -> {
                if (TAG_ATTR == xmlPullParser.name) {
                    attr = null
                }
            }
        }
        eventType = xmlPullParser.next()
    }

    return map
}
