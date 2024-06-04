package com.jirkastudio.wearostasks.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults.ItemType
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.jirkastudio.shared.MyTask
import com.jirkastudio.wearostasks.R


class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var taskList: MutableList<MyTask>
    private val viewModel: TaskViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        taskList = mutableListOf()

        setContent {
            TaskApp()
        }
        viewModel.loadTasks(applicationContext)
    }

    @OptIn(ExperimentalHorologistApi::class)
    @Composable
    fun TaskApp() {
        val tasks = viewModel.taskList.observeAsState(initial = emptyList()).value
        val loading = viewModel.isLoading.observeAsState(initial = true).value

        MaterialTheme {
            AppScaffold {
                val listState = rememberResponsiveColumnState(
                    contentPadding = ScalingLazyColumnDefaults.padding(
                        first = ItemType.Text,
                        last = ItemType.Chip,
                    ),
                )
                val contentModifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                ScreenScaffold(
                    scrollState = listState,
                ) {
                    if (loading || tasks.isEmpty()) {
                        val text = if (loading) {
                            stringResource(R.string.loading)
                        } else {
                            stringResource(R.string.all_done_nothing_to_do)
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text, style = TextStyle(fontSize = 20.sp), textAlign = TextAlign.Center)
                        }
                    } else {
                        ScalingLazyColumn(
                            columnState = listState,
                        ) {
                            item {
                                Text(stringResource(R.string.today_tasks), style = TextStyle(fontSize = 20.sp), textAlign = TextAlign.Center, modifier = contentModifier)
                            }

                            items(tasks) { task ->
                                CustomToggleChip(
                                    checked = task.isCompleted,
                                    onCheckedChange = { viewModel.markAsCompleted(task) },
                                    labelText = task.name,
                                    modifier = contentModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "Listening for messages")
        Wearable.getMessageClient(this).addListener(this)
        viewModel.loadTasks(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "Stopped listening for messages")
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("Listener invoked", "Message received")
        if (messageEvent.path == "/tasks") {
            val message = String(messageEvent.data)
            Log.d("MainActivity", "Received message with tasks")
            viewModel.updateTasks(message)
        } else if (messageEvent.path == "/responsePath") {
            Log.d("MainActivity", "Confirmation received")
        }
    }
}