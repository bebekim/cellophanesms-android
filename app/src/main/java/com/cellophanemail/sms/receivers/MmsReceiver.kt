package com.cellophanemail.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            return
        }

        Log.d(TAG, "MMS received - MMS handling will be implemented in Phase 2")

        // TODO: Implement MMS handling in Phase 2
        // For now, MMS messages are received but not processed through the filtering pipeline
        // This ensures the app can still function as default SMS handler
    }

    companion object {
        private const val TAG = "MmsReceiver"
    }
}
