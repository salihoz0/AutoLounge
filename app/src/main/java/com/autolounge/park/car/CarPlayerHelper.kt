package com.autohub.park.car

import com.autohub.park.i18n.tr
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper utility to manage Media3 PlayerView controls and popup dialogs in Android Auto.
 * Fixes orphaned settings window ("Hız / Ses") by providing outside touch dismissal,
 * toggle behavior on the gear icon, an explicit "Kapat" list option, and sync with controller hide.
 */
object CarPlayerHelper {
    private const val TAG = "CarPlayerHelper"

    /**
     * Inspects and returns the internal PopupWindow used by Media3 PlayerControlView for settings.
     */
    fun getSettingsPopup(playerView: PlayerView?): PopupWindow? {
        val pv = playerView ?: return null
        return try {
            val controlView = pv.findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller) ?: return null
            val field = PlayerControlView::class.java.getDeclaredField("settingsWindow").apply {
                isAccessible = true
            }
            field.get(controlView) as? PopupWindow
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Dismisses the settings popup window if it is currently displayed.
     * If [touchX] and [touchY] are specified, only dismisses if the touch occurred outside the popup bounds.
     * Returns true if the popup was dismissed.
     */
    fun dismissSettingsIfOpen(playerView: PlayerView?, touchX: Float = -1f, touchY: Float = -1f): Boolean {
        val popup = getSettingsPopup(playerView) ?: return false
        if (!popup.isShowing) return false
        val contentView = popup.contentView ?: run {
            try { popup.dismiss() } catch (_: Throwable) {}
            return true
        }

        if (touchX >= 0f && touchY >= 0f) {
            val location = IntArray(2)
            contentView.getLocationOnScreen(location)
            val left = location[0]
            val top = location[1]
            val right = left + contentView.width
            val bottom = top + contentView.height
            // If the touch occurred inside the popup, keep it open for user clicks
            if (touchX >= left && touchX <= right && touchY >= top && touchY <= bottom) {
                return false
            }
        }

        try {
            popup.dismiss()
        } catch (_: Throwable) {}
        return true
    }

    /**
     * Enhances a PlayerView with:
     * 1. Direct "Kapat" (Close) item in the settings list
     * 2. Outside tap dismissal
     * 3. Gear icon toggle behavior (tap to open, tap again to close)
     * 4. Controller visibility listener (auto-dismiss when player controls hide)
     * 5. Auto-dismiss timer (e.g. 6 seconds of inactivity)
     */
    fun setupDismissableSettings(
        playerView: PlayerView,
        scope: CoroutineScope,
        autoDismissTimeoutMs: Long = 6000L
    ) {
        var autoDismissJob: Job? = null

        val scheduleAutoDismiss: () -> Unit = {
            autoDismissJob?.cancel()
            autoDismissJob = scope.launch {
                delay(autoDismissTimeoutMs)
                dismissSettingsIfOpen(playerView)
            }
        }

        playerView.post {
            try {
                val controlView = playerView.findViewById<PlayerControlView>(androidx.media3.ui.R.id.exo_controller) ?: return@post

                // 1. Controller visibility: when controls fade out, dismiss popup so it never stays orphaned
                controlView.addVisibilityListener { visibility ->
                    if (visibility != View.VISIBLE) {
                        dismissSettingsIfOpen(playerView)
                        autoDismissJob?.cancel()
                    }
                }

                // 2. Add "Kapat" option to SettingsAdapter
                injectCloseOptionIntoSettingsAdapter(controlView, playerView.context)

                // 3. Make Settings button toggleable on touch
                val settingsBtn = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_settings)
                settingsBtn?.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val popup = getSettingsPopup(playerView)
                        if (popup != null && popup.isShowing) {
                            popup.dismiss()
                            autoDismissJob?.cancel()
                            return@setOnTouchListener true
                        }
                    }
                    false
                }

                // 4. Hook popup creation/show to enable outside touch & start auto-dismiss timer
                val defaultListener: View.OnClickListener? = try {
                    val field = PlayerControlView::class.java.getDeclaredField("componentListener").apply {
                        isAccessible = true
                    }
                    field.get(controlView) as? View.OnClickListener
                } catch (_: Throwable) {
                    null
                }

                settingsBtn?.setOnClickListener { v ->
                    val popup = getSettingsPopup(playerView)
                    if (popup != null && popup.isShowing) {
                        popup.dismiss()
                        autoDismissJob?.cancel()
                        return@setOnClickListener
                    }
                    defaultListener?.onClick(v)
                    playerView.postDelayed({
                        val p = getSettingsPopup(playerView)
                        if (p != null && p.isShowing) {
                            p.isOutsideTouchable = true
                            p.isFocusable = true
                            p.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            p.setOnDismissListener {
                                autoDismissJob?.cancel()
                            }
                            scheduleAutoDismiss()
                        }
                    }, 60)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "setupDismissableSettings error: ${e.message}")
            }
        }
    }

    private fun injectCloseOptionIntoSettingsAdapter(controlView: PlayerControlView, context: Context) {
        try {
            val adapterField = PlayerControlView::class.java.getDeclaredField("settingsAdapter").apply {
                isAccessible = true
            }
            val adapter = adapterField.get(controlView) ?: return
            val adapterClass = adapter.javaClass

            val mainTextsField = adapterClass.getDeclaredField("mainTexts").apply { isAccessible = true }
            val subTextsField = adapterClass.getDeclaredField("subTexts").apply { isAccessible = true }
            val iconIdsField = adapterClass.getDeclaredField("iconIds").apply { isAccessible = true }

            @Suppress("UNCHECKED_CAST")
            val originalMain = mainTextsField.get(adapter) as? Array<String> ?: return
            @Suppress("UNCHECKED_CAST")
            val originalSub = subTextsField.get(adapter) as? Array<String?> ?: return
            @Suppress("UNCHECKED_CAST")
            val originalIcons = iconIdsField.get(adapter) as? Array<Drawable?> ?: return

            // If already injected, don't duplicate
            if (originalMain.contains(tr("Kapat"))) return

            val newMain = Array(originalMain.size + 1) { i ->
                if (i < originalMain.size) originalMain[i] else tr("Kapat")
            }
            val newSub = Array<String?>(originalSub.size + 1) { i ->
                if (i < originalSub.size) originalSub[i] else null
            }
            val closeIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel)?.apply {
                setTint(Color.parseColor("#EF4444"))
            }
            val newIcons = Array<Drawable?>(originalIcons.size + 1) { i ->
                if (i < originalIcons.size) originalIcons[i] else closeIcon
            }

            mainTextsField.set(adapter, newMain)
            subTextsField.set(adapter, newSub)
            iconIdsField.set(adapter, newIcons)
            try {
                adapterClass.getMethod("notifyDataSetChanged").invoke(adapter)
            } catch (_: Throwable) {}
        } catch (e: Throwable) {
            Log.w(TAG, "Cannot inject close option: ${e.message}")
        }
    }
}
