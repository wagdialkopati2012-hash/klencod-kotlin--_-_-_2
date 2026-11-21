package com.example

import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MunakasatApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    requestHighRefreshRate()
    val appContainer = (application as TenderApplication).container
    setContent { MunakasatApp(container = appContainer) }
  }

  override fun onResume() {
    super.onResume()
    requestHighRefreshRate()
  }

  /**
   * Asks the system to run this window at the highest refresh rate the screen supports (up to 120Hz)
   * at the current resolution, so animations and scrolling are rendered at 120 FPS on devices that
   * support it. On 60Hz screens this is a harmless no-op.
   */
  private fun requestHighRefreshRate() {
    runCatching {
      val currentDisplay: Display? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          this.display
        } else {
          @Suppress("DEPRECATION") windowManager.defaultDisplay
        }
      if (currentDisplay == null) return@runCatching

      val currentMode = currentDisplay.mode
      val sameResolution =
        currentDisplay.supportedModes.filter {
          it.physicalWidth == currentMode.physicalWidth &&
            it.physicalHeight == currentMode.physicalHeight
        }
      val upTo120 = sameResolution.filter { it.refreshRate <= 120.5f }
      val best = (upTo120.ifEmpty { sameResolution }).maxByOrNull { it.refreshRate }
      if (best != null) {
        val attrs = window.attributes
        attrs.preferredDisplayModeId = best.modeId
        attrs.preferredRefreshRate = best.refreshRate
        window.attributes = attrs
      }
    }
  }
}