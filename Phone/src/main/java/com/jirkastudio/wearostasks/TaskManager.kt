package com.jirkastudio.wearostasks

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.property.RRule
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TaskManager(private val context: Context) {
    private val contentUri = Uri.parse("content://org.tasks.opentasks/tasks")
    fun loadTasks(): String {
        var taskView = ""
        val tasks = mutableListOf<Map<String, String>>()

        val contentResolver: ContentResolver = context.contentResolver

        val calendar = Calendar.getInstance()


        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis

        val selection = "(completed = ? OR completed IS NULL) AND (due <= ? OR dtstart <= ?)"
        val selectionArgs = arrayOf("0", endOfDay.toString(), endOfDay.toString())

        val projection = arrayOf("_id", "title")

        val cursor: Cursor? =
            contentResolver.query(contentUri, projection, selection, selectionArgs, null)

        if (cursor != null) {
            while (cursor.moveToNext()) {
                val task = mutableMapOf<String, String>()
                for (i in 0 until cursor.columnCount) {
                    val columnName = cursor.getColumnName(i)
                    val value = if (!cursor.isNull(i)) cursor.getString(i) else ""
                    task[columnName] = value
                }
                tasks.add(task)
            }
            cursor.close()

            taskView += "\n"
            for (task in tasks) {
                for ((_, value) in task) {
                    taskView += value.replace("\n", " ") + "\n"
                }
            }
        }
        Log.d("MessageService", "Tasks loaded: $taskView")

        return taskView
    }

    private fun getNextOccurrence(
        rruleString: String, dueMillis: Long
    ): Triple<Long, String?, Boolean> {
        try {
            val rrule = RRule<LocalDateTime>(rruleString)
            val recur = rrule.recur

            val dueInstant = Instant.ofEpochMilli(dueMillis)
            val dueDateTime = LocalDateTime.ofInstant(dueInstant, ZoneId.systemDefault())

            val nextDateTime =
                recur.getNextDateTime(dueDateTime) ?: return Triple(dueMillis, rruleString, true)
            val nextOccurrenceMillis =
                nextDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            var updatedRRule: String? = null
            var isLastOccurrence = false

            if (recur.count > 0) {
                val newCount = recur.count - 1
                updatedRRule = if (newCount > 0) {
                    rruleString.replace("COUNT=${recur.count}", "COUNT=$newCount")
                } else {
                    isLastOccurrence = true
                    rruleString.replace("COUNT=${recur.count}", "")
                }
            }

            return Triple(nextOccurrenceMillis, updatedRRule, isLastOccurrence)
        } catch (e: Exception) {
            e.printStackTrace()
            return Triple(0, null, false)
        }
    }

    private fun Recur<LocalDateTime>.getNextDateTime(fromDateTime: LocalDateTime): LocalDateTime? {
        val recurrenceDates = generateSequence(fromDateTime) { previousDate ->
            val nextDate = this.getNextDate(previousDate, fromDateTime)
            nextDate?.let {
                LocalDateTime.ofInstant(
                    it.toInstant(ZonedDateTime.now(ZoneId.systemDefault()).offset),
                    ZoneId.systemDefault()
                )
            } ?: return@generateSequence null
        }.drop(1).firstOrNull()

        return recurrenceDates
    }

    @SuppressLint("Range")
    fun completeTask(taskId: String) {
        val selection = "_id = ?"
        val selectionArgs = arrayOf(taskId)

        val projection = arrayOf("rrule, due")
        val cursor: Cursor? =
            context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)
        if (cursor != null && cursor.moveToFirst()) {
            val rrule = cursor.getString(cursor.getColumnIndex("rrule"))
            val due = cursor.getLong(cursor.getColumnIndex("due"))
            cursor.close()

            // If the task has an rrule, reschedule it
            if (!rrule.isNullOrEmpty()) {
                val (nextOccurrenceMillis, updatedRRule, isLastOccurrence) = getNextOccurrence(
                    rrule, due
                )

                if (updatedRRule == null) {
                    Log.d("TaskManager", "Error calculating next occurrence.")
                    return
                }

                val updateValues = ContentValues()
                updateValues.put("due", nextOccurrenceMillis)
                updateValues.put("rrule", updatedRRule)
                if (!isLastOccurrence) {
                    val rowsUpdated = context.contentResolver.update(
                        contentUri, updateValues, selection, selectionArgs
                    )
                    if (rowsUpdated > 0) {
                        Log.d("TaskManager", "Task rescheduled.")
                    } else {
                        Log.d("TaskManager", "Task not found or already completed.")
                    }
                    return
                }

            }

            // If the task does not have an rrule, mark it as completed
            val currentTime = System.currentTimeMillis().toString()

            val contentValues = ContentValues()
            contentValues.put("completed", currentTime)

            val rowsUpdated = context.contentResolver.update(
                contentUri, contentValues, selection, selectionArgs
            )

            if (rowsUpdated > 0) {
                Log.d("TaskManager", "Task marked as completed.")
            } else {
                Log.d("TaskManager", "Task not found or already completed.")
            }
        }
    }
}