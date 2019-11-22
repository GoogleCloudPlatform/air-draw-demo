package com.jamesward.airdraw

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.context.env.PropertySource

class BaseApplication: Application() {

    private var ctx: ApplicationContext? = null

    override fun onCreate() {
        super.onCreate()

        val ai = applicationContext.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val propertySource = AndroidMetadataPropertySource(ai.metaData)

        ctx = ApplicationContext.build(MainActivity::class.java, Environment.ANDROID).propertySources(propertySource).start()

        registerActivityLifecycleCallbacks(object: ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
                if (activity != null) ctx?.inject(activity)
            }

            override fun onActivityPaused(activity: Activity?) { }
            override fun onActivityResumed(activity: Activity?) { }
            override fun onActivityStarted(activity: Activity?) { }
            override fun onActivityDestroyed(activity: Activity?) { }
            override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) { }
            override fun onActivityStopped(activity: Activity?) { }
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        ctx?.let { ctx ->
            if (ctx.isRunning) ctx.stop()
        }
    }
}

class AndroidMetadataPropertySource(private val bundle: Bundle): PropertySource {
    override fun getName(): String {
        return javaClass.simpleName
    }

    override fun iterator(): MutableIterator<String> {
        return bundle.keySet().iterator()
    }

    override fun get(key: String?): Any? {
        return bundle.get(key)
    }
}
