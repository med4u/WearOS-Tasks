package com.jirkastudio.wearostasks

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val _permissionRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.Theme_WearOSTasks)
        if (ContextCompat.checkSelfPermission(
                this, "org.tasks.permission.READ_TASKS"
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, "org.tasks.permission.WRITE_TASKS"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    "org.tasks.permission.READ_TASKS", "org.tasks.permission.WRITE_TASKS"
                ), _permissionRequestCode
            )
        } else {
            setContentView(R.layout.activity_main)
            val taskView: TextView = findViewById(R.id.taskView)
            taskView.text = getString(R.string.everything_looks_fine)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        setContentView(R.layout.activity_main)
        val taskView: TextView = findViewById(R.id.taskView)
        if (requestCode == _permissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                taskView.text = getString(R.string.everything_looks_fine)
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(getString(R.string.app_will_not_work))
                    .setCancelable(false)
                    .setPositiveButton("OK") { _, _ -> finish() }
                val alert = builder.create()
                alert.show()
            }
        }
    }
}
