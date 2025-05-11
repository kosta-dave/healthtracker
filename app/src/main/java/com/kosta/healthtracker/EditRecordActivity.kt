package com.kosta.healthtracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.kosta.healthtracker.data.HealthRecord
import com.kosta.healthtracker.databinding.ActivityEditRecordBinding
import com.kosta.healthtracker.viewmodel.HealthRecordViewModel
import com.kosta.healthtracker.viewmodel.HealthRecordViewModelFactory
import com.kosta.healthtracker.data.AppDatabase
import com.kosta.healthtracker.data.HealthRecordRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.app.ActivityOptions


class EditRecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditRecordBinding
    private lateinit var viewModel: HealthRecordViewModel
    private var currentRecord: HealthRecord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Включить кнопку "Назад" в ActionBar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayUseLogoEnabled(true)
            setLogo(R.drawable.ic_logo)
        }


        // Инициализация ViewModel
        val dao = AppDatabase.getInstance(application).healthRecordDao()
        viewModel = ViewModelProvider(
            this,
            HealthRecordViewModelFactory(HealthRecordRepository(dao))
        )[HealthRecordViewModel::class.java]

        // Проверка, редактируем ли существующую запись
        val recordId = intent.getLongExtra("record_id", -1L)
        if (recordId != -1L) {
            lifecycleScope.launch {
                viewModel.allRecords.first { records ->
                    records.firstOrNull { it.id == recordId }?.let { record ->
                        currentRecord = record
                        fillForm(record)
                        binding.btnDelete.visibility = View.VISIBLE
                    }
                    true
                }
            }
        }

        // Установка текущей даты по умолчанию
        binding.etDate.setText(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()))

        // Обработчики событий
        setupTimeInputValidation()
        setupListeners()
    }


    // Обработка нажатия на кнопку в ActionBar
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition() // или supportFinishAfterTransition()
        return true
    }

    private fun fillForm(record: HealthRecord) {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(record.date))
        binding.etGlucose.setText(record.glucose?.toString() ?: "")
        binding.etKetones.setText(record.ketones?.toString() ?: "")
        binding.etWaistSize.setText(record.waistSize?.toString() ?: "")
        binding.etComment.setText(record.comment ?: "")
        binding.etSteps.setText(record.steps?.toString() ?: "")
        binding.etBreakfast.setText(record.breakfastTime ?: "")
        binding.etLunch.setText(record.lunchTime ?: "")
        binding.etDinner.setText(record.dinnerTime ?: "")
    }

    private fun setupTimeInputValidation() {
        val timeTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val input = s.toString()
                if (input.length >= 2) {
                    val hours = input.substring(0, 2).toIntOrNull()
                    if (hours != null && hours > 23) {
                        s.replace(0, 2, "23")
                    }
                }
                if (input.length >= 5) {
                    val minutes = input.substring(3, 5).toIntOrNull()
                    if (minutes != null && minutes > 59) {
                        s.replace(3, 5, "59")
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        binding.etBreakfast.addTextChangedListener(timeTextWatcher)
        binding.etLunch.addTextChangedListener(timeTextWatcher)
        binding.etDinner.addTextChangedListener(timeTextWatcher)
    }

    private fun setupListeners() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    binding.etDate.setText("%02d.%02d.%04d".format(day, month + 1, year))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveRecord()
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
            val options = ActivityOptions.makeCustomAnimation(
                this@EditRecordActivity,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }
    }

    private fun validateForm(): Boolean {
        val hasData = binding.etGlucose.text.isNotBlank() ||
                binding.etKetones.text.isNotBlank() ||
                binding.etWaistSize.text.isNotBlank() ||
                binding.etSteps.text.isNotBlank() ||
                binding.etBreakfast.text.isNotBlank() ||
                binding.etLunch.text.isNotBlank() ||
                binding.etDinner.text.isNotBlank()

        // Проверяем корректность времени
        val validTimes = listOf(
            binding.etBreakfast.text.toString(),
            binding.etLunch.text.toString(),
            binding.etDinner.text.toString()
        ).all { time ->
            time.isBlank() || formatMealTime(time) != null
        }

        if (!validTimes) {
            Toast.makeText(this, "Некорректное время (часы 0-23, минуты 0-59)", Toast.LENGTH_SHORT).show()
        }

        return hasData && validTimes
    }

    private fun saveRecord() {
        val utilDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .parse(binding.etDate.text.toString()) ?: Date()

        // Конвертируем java.util.Date в java.sql.Date
        val sqlDate = java.sql.Date(utilDate.time)

        lifecycleScope.launch {
            // Проверяем есть ли уже запись с такой датой
            val existingRecord = viewModel.getRecordByDate(sqlDate)

            if (existingRecord != null && existingRecord.id != currentRecord?.id) {
                // Нашли запись с такой датой, которая НЕ является текущей редактируемой
                showDuplicateDateDialog(existingRecord)
                return@launch
            }

            // Продолжаем сохранение, если дубликата нет
            val breakfastTime = formatMealTime(binding.etBreakfast.text.toString())
            val lunchTime = formatMealTime(binding.etLunch.text.toString())
            val dinnerTime = formatMealTime(binding.etDinner.text.toString())

            val record = currentRecord?.copy(
                date = sqlDate,
                glucose = binding.etGlucose.text.toString().toFloatOrNull(),
                ketones = binding.etKetones.text.toString().toFloatOrNull(),
                waistSize = binding.etWaistSize.text.toString().toFloatOrNull(),
                comment = binding.etComment.text.toString().takeIf { it.isNotBlank() },
                steps = binding.etSteps.text.toString().toIntOrNull(),
                breakfastTime = breakfastTime?.takeIf { it.isNotBlank() },
                lunchTime = lunchTime?.takeIf { it.isNotBlank() },
                dinnerTime = dinnerTime?.takeIf { it.isNotBlank() }
            ) ?: HealthRecord(
                date = sqlDate,
                glucose = binding.etGlucose.text.toString().toFloatOrNull(),
                ketones = binding.etKetones.text.toString().toFloatOrNull(),
                waistSize = binding.etWaistSize.text.toString().toFloatOrNull(),
                comment = binding.etComment.text.toString().takeIf { it.isNotBlank() },
                steps = binding.etSteps.text.toString().toIntOrNull(),
                breakfastTime = breakfastTime?.takeIf { it.isNotBlank() },
                lunchTime = lunchTime?.takeIf { it.isNotBlank() },
                dinnerTime = dinnerTime?.takeIf { it.isNotBlank() }
            )

            if (currentRecord == null) {
                viewModel.insert(record)
            } else {
                viewModel.update(record)
            }

            Toast.makeText(this@EditRecordActivity, "Данные сохранены", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDuplicateDateDialog(existingRecord: HealthRecord) {
        AlertDialog.Builder(this)
            .setTitle("Дублирование записи")
            .setMessage("Запись на эту дату уже существует. Хотите перейти к ней?")
            .setPositiveButton("Да") { _, _ ->
                val intent = Intent(this, EditRecordActivity::class.java).apply {
                    putExtra("record_id", existingRecord.id)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun formatMealTime(input: String?): String? {
        if (input.isNullOrBlank()) return null

        val digits = input.replace("\\D".toRegex(), "")
        if (digits.isEmpty()) return null

        // Проверяем и корректируем часы и минуты
        val (hours, minutes) = when (digits.length) {
            1 -> Pair(digits.substring(0, 1).toIntOrNull()?.coerceIn(0..23), 0)
            2 -> Pair(digits.substring(0, 2).toIntOrNull()?.coerceIn(0..23), 0)
            3 -> {
                val h = digits.substring(0, 2).toIntOrNull()?.coerceIn(0..23)
                val m = digits.substring(2, 3).toIntOrNull()?.let { it * 10 }?.coerceIn(0..59)
                h to m
            }
            else -> {
                val h = digits.substring(0, 2).toIntOrNull()?.coerceIn(0..23)
                val m = digits.substring(2, 4).toIntOrNull()?.coerceIn(0..59)
                h to m
            }
        }

        return if (hours != null && minutes != null) {
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        } else {
            null
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Удаление записи")
            .setMessage("Вы уверены, что хотите удалить эту запись?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    currentRecord?.let { viewModel.delete(it) }
                    Toast.makeText(this@EditRecordActivity, "Запись удалена", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}