package com.vikram.expressiveisland.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Backs the buttons on the in-app test call ([TestCaller]): decline / hang-up ends the fake call by
 * clearing [com.vikram.expressiveisland.core.OnCallBus] (so the phone tile dismisses just as it
 * would when a real dialer removes its ongoing-call notification), and answer connects it.
 */
class TestCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_END -> TestCaller.end()
            ACTION_ANSWER -> TestCaller.answer(context)
        }
    }

    companion object {
        const val ACTION_END = "com.ekoehler.expressiveisland.action.TEST_END_CALL"
        const val ACTION_ANSWER = "com.ekoehler.expressiveisland.action.TEST_ANSWER_CALL"
    }
}
