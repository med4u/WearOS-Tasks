package com.jirkastudio.wearostasks

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class MessageService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("Listener invoked", messageEvent.path)
        if (messageEvent.path == "/tasks_requests") {
            Log.d("MessageService", "Confirmation received")
            val responseContent = TaskManager(this).loadTasks().toByteArray()
            Wearable.getMessageClient(applicationContext)
                .sendMessage(messageEvent.sourceNodeId, "/tasks", responseContent)
                .apply {
                    addOnSuccessListener { Log.d("MessageService", "Response sent successfully") }
                    addOnFailureListener { Log.d("MessageService", "Failed to send response") }
                }
        } else if (messageEvent.path == "/complete_task") {
            val id = String(messageEvent.data)
            TaskManager(this).completeTask(id)
        }
    }
}