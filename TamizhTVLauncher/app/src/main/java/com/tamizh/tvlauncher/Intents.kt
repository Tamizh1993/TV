package com.tamizh.tvlauncher

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Starts an activity without crashing when the TV doesn't support it
 * (many TV builds strip out some settings screens). Returns true on success.
 */
fun Context.startSafely(intent: Intent, fallback: Intent? = null): Boolean {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    } catch (e: Exception) {
        if (fallback != null) {
            try {
                startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e2: Exception) {
                // fall through to the toast
            }
        }
    }
    Toast.makeText(this, "Not available on this TV", Toast.LENGTH_SHORT).show()
    return false
}
