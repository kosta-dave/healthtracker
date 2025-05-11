package com.kosta.healthtracker

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.kosta.healthtracker.databinding.ActivityImportExportBinding
import com.kosta.healthtracker.viewmodel.HealthRecordViewModel
import com.kosta.healthtracker.data.AppDatabase
import com.kosta.healthtracker.data.HealthRecord
import com.kosta.healthtracker.data.HealthRecordRepository
import com.kosta.healthtracker.viewmodel.HealthRecordViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImportExportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImportExportBinding
    private lateinit var viewModel: HealthRecordViewModel
    private val gson = Gson()
    private val TAG = "ImportExport"

    // Добавьте эти константы
    companion object {
        private const val REQUEST_CODE_STORAGE_PERMISSION = 1001
        private const val REQUEST_CODE_MANAGE_STORAGE = 1002
    }

    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "Selected file URI: $uri")
                handleImportedFile(uri)
            } ?: run {
                Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Обработка нажатия на кнопку в ActionBar
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition() // или supportFinishAfterTransition()
        return true
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity created")
        try {
            binding = ActivityImportExportBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val dao = AppDatabase.getInstance(application).healthRecordDao()
            viewModel = ViewModelProvider(
                this,
                HealthRecordViewModelFactory(HealthRecordRepository(dao))
            ).get(HealthRecordViewModel::class.java)

            binding.btnExport.setOnClickListener {
                Log.d(TAG, "Export button clicked")
                checkStoragePermissions()
            }

            binding.btnImport.setOnClickListener {
                Log.d(TAG, "Import button clicked")
                importData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Init error", e)
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun checkStoragePermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    exportData()
                } else {
                    requestManageStoragePermission()
                }
            }
            else -> {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    exportData()
                } else {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_CODE_STORAGE_PERMISSION
                    )
                }
            }
        }
    }

    private fun requestManageStoragePermission() {
        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    exportData()
                } else {
                    Toast.makeText(
                        this,
                        "Необходимо разрешение для экспорта данных",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_MANAGE_STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    Environment.isExternalStorageManager()) {
                    exportData()
                }
            }
        }
    }

    private fun exportData() {
        lifecycleScope.launch {
            try {
                val records = viewModel.allRecords.first()
                if (records.isEmpty()) {
                    Toast.makeText(this@ImportExportActivity,
                        "Нет данных для экспорта",
                        Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val jsonString = GsonBuilder().setPrettyPrinting().create()
                    .toJson(records)
                val fileName = "health_records_${SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
                ).format(Date())}.json"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Для Android 10+ используем MediaStore
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS)
                    }
                    contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                    )?.let { uri ->
                        contentResolver.openOutputStream(uri)?.use {
                            it.write(jsonString.toByteArray())
                            showExportSuccess(fileName)
                        }
                    }
                } else {
                    // Для старых версий Android
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName).apply {
                        parentFile?.mkdirs()
                    }
                    file.writeText(jsonString)
                    showExportSuccess(fileName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                Toast.makeText(
                    this@ImportExportActivity,
                    "Ошибка экспорта: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showExportSuccess(fileName: String) {
        runOnUiThread {
            AlertDialog.Builder(this@ImportExportActivity)
                .setTitle("Экспорт завершен")
                .setMessage("Файл сохранен: $fileName")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    // Остальные методы (importData, handleImportedFile) остаются без изменений
    // ...

    private fun importData() {
        try {
            Log.d(TAG, "Starting file picker")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            importFileLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "File picker error", e)
            Toast.makeText(
                this,
                "Ошибка при выборе файла",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun handleImportedFile(uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Processing imported file")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().readText()

                    val records = try {
                        gson.fromJson(jsonString, Array<HealthRecord>::class.java).toList()
                    } catch (e: JsonSyntaxException) {
                        Toast.makeText(
                            this@ImportExportActivity,
                            "Неверный формат файла",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    if (records.isNotEmpty()) {
                        records.forEach { viewModel.insert(it) }
                        Toast.makeText(
                            this@ImportExportActivity,
                            "Импортировано ${records.size} записей",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@ImportExportActivity,
                            "Файл не содержит данных",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } ?: run {
                    Toast.makeText(
                        this@ImportExportActivity,
                        "Не удалось прочитать файл",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Import processing failed", e)
                Toast.makeText(
                    this@ImportExportActivity,
                    "Ошибка импорта: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}