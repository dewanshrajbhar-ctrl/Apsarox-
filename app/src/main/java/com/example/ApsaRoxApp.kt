package com.example

import android.app.Application
import android.system.Os

class ApsaRoxApp : Application() {
    companion object {
        init {
            configureSoftwareRendering()
        }

        private fun configureSoftwareRendering() {
            try {
                // Prevent MESA from attempting to open unavailable DRM rendernodes (/dev/dri/renderD128)
                Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
                Os.setenv("MESA_DEBUG", "0", true)
                Os.setenv("MESA_LOG_LEVEL", "silent", true)
            } catch (e: Throwable) {
                // Ignore if environment cannot be modified
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        configureSoftwareRendering()
    }
}
