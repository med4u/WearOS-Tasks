package com.jirkastudio.wearostasks.presentation

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.jirkastudio.shared.MyTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel : ViewModel() {

    private var applicationContext : Context? = null
    private val _taskList = MutableLiveData<List<MyTask>>()
    val taskList: LiveData<List<MyTask>> get() = _taskList
    val isLoading = MutableLiveData(true)

    fun loadTasks(context: Context) {
        applicationContext = context
        viewModelScope.launch(Dispatchers.IO) {
            val nodes = getNodes(applicationContext!!)
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/tasks_requests", null)
                    .apply {
                        addOnSuccessListener { Log.d("MainActivity", "Message sent successfully") }
                        addOnFailureListener { Log.d("MainActivity", "Failed to send message") }
                    }
            }
        }
    }

    fun updateTasks(message: String) {
        isLoading.postValue(false)
        val lines = message.split("\n").filter { it.isNotBlank() }
        val tasks = lines.chunked(2).map {
            MyTask(name = it[1], id = it[0], isCompleted = false)
        }
        _taskList.postValue(tasks)
    }

    private suspend fun getNodes(applicationContext: Context): List<Node> =
        withContext(Dispatchers.IO) {
            val nodeList: List<Node> =
                Tasks.await(Wearable.getNodeClient(applicationContext).connectedNodes)
            nodeList
        }

    private fun sendCompleteMessage(id: String){
        viewModelScope.launch(Dispatchers.IO) {
            val nodes = getNodes(applicationContext!!)
            if (nodes.isNotEmpty()) {
                val node = nodes.first()
                Wearable.getMessageClient(applicationContext!!)
                    .sendMessage(node.id, "/complete_task", id.toByteArray())
                    .apply {
                        addOnSuccessListener { Log.d("MainActivity", "Message sent successfully") }
                        addOnFailureListener { Log.d("MainActivity", "Failed to send message") }
                    }
            }
        }
    }
    fun markAsCompleted(myTask: MyTask) {
        val updatedTasks = taskList.value!!.map {
            if (it.id == myTask.id) {
                if (!it.isCompleted) {
                    sendCompleteMessage(myTask.id)
                    it.copy(isCompleted = true)
                } else {
                    return@map it
                }
            } else {
                it
            }
        }
        _taskList.postValue(updatedTasks)
    }
}