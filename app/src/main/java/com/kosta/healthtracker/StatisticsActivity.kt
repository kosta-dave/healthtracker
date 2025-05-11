package com.kosta.healthtracker

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.kosta.healthtracker.data.AppDatabase
import com.kosta.healthtracker.data.HealthRecord
import com.kosta.healthtracker.data.HealthRecordRepository
import com.kosta.healthtracker.databinding.ActivityStatisticsBinding
import com.kosta.healthtracker.viewmodel.HealthRecordViewModel
import com.kosta.healthtracker.viewmodel.HealthRecordViewModelFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.res.Configuration
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.charts.LineChart


class StatisticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var viewModel: HealthRecordViewModel
    private val dateFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
    private val weekFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
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

        // Настройка UI
        setupUI()

        // Загрузка данных
        loadData()
    }

    private fun setupYAxis(chart: LineChart, minValue: Float, maxValue: Float) {
        val padding = (maxValue - minValue) * 0.1f // 10% от диапазона

        chart.axisLeft.apply {
            resetAxisMinimum() // Сброс предыдущих значений
            resetAxisMaximum()

            // Устанавливаем границы с отступами
            axisMinimum = minValue - padding
            axisMaximum = maxValue + padding

            granularity = (maxValue - minValue) / 5f // 5 делений на оси
            setDrawGridLines(true)
            setDrawAxisLine(true)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        loadData() // Перезагружаем данные при смене темы
    }

    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition()
        return true
    }

    private fun setupUI() {
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioWeek -> loadData(7)
                R.id.radioMonth -> loadData(30)
                R.id.radioAll -> loadData(0)
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadData(days: Int = 7) {
        lifecycleScope.launch {
            val allRecords = viewModel.allRecords.first()
            val filteredRecords = if (days > 0) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -days)
                val startDate = calendar.time
                allRecords.filter { it.date.after(startDate) || it.date == startDate }
            } else {
                allRecords
            }.sortedBy { it.date }

            updateCharts(filteredRecords)
            updateStatsSummary(filteredRecords)
        }
    }

    private fun updateCharts(records: List<HealthRecord>) {
        setupGlucoseChart(records)
        setupStepsChart(records)
        setupWaistChart(records)
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyChartTheme(chart: LineChart) {
        val textColor = if (isDarkTheme()) Color.WHITE else Color.BLACK
        val gridColor = if (isDarkTheme()) Color.GRAY else Color.LTGRAY

        chart.apply {
            setBackgroundColor(Color.TRANSPARENT)
            description.isEnabled = false

            // Настройка легенды
            legend.apply {
                this.textColor = textColor
                textSize = 12f
                formSize = 12f
                form = Legend.LegendForm.CIRCLE
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            // Настройка оси X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                this.textColor = textColor
                this.gridColor = gridColor
                axisLineColor = gridColor
                setDrawGridLines(true)
                setDrawAxisLine(true)
            }

            // Настройка левой оси Y
            axisLeft.apply {
                this.textColor = textColor
                this.gridColor = gridColor
                axisLineColor = gridColor
                setDrawGridLines(true)
                setDrawAxisLine(true)
            }

            // Отключаем правую ось Y
            axisRight.isEnabled = false

            setNoDataTextColor(textColor)
        }
    }

    private fun setupGlucoseChart(records: List<HealthRecord>) {
        val glucoseEntries = mutableListOf<Entry>()
        val ketonesEntries = mutableListOf<Entry>()
        val dates = mutableListOf<String>()
        val glucoseValues = mutableListOf<Float>()
        val ketonesValues = mutableListOf<Float>()

        records.forEachIndexed { index, record ->
            record.glucose?.let { glucose ->
                glucoseEntries.add(Entry(index.toFloat(), glucose))
                glucoseValues.add(glucose)
            }
            record.ketones?.let { ketones ->
                ketonesEntries.add(Entry(index.toFloat(), ketones))
                ketonesValues.add(ketones)
            }
            dates.add(dateFormat.format(record.date))
        }

        val textColor = if (isDarkTheme()) Color.WHITE else Color.BLACK

        val glucoseDataSet = LineDataSet(glucoseEntries, "Глюкоза (ммоль/л)").apply {
            color = Color.RED
            lineWidth = 2f
            setCircleColor(Color.RED)
            circleRadius = 4f
            valueTextSize = 10f
            valueTextColor = textColor
            setDrawValues(true)
        }

        val ketonesDataSet = LineDataSet(ketonesEntries, "Кетоны (ммоль/л)").apply {
            color = Color.BLUE
            lineWidth = 2f
            setCircleColor(Color.BLUE)
            circleRadius = 4f
            valueTextSize = 10f
            valueTextColor = textColor
            setDrawValues(true)
        }

        val lineData = LineData(glucoseDataSet, ketonesDataSet)
        binding.glucoseChart.apply {
            applyChartTheme(this)

            // Настройка оси X
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dates.getOrNull(value.toInt()) ?: ""
                }
            }

            // Настройка оси Y для глюкозы/кетонов
            axisLeft.apply {
                // Объединяем все значения для расчета границ
                val allValues = glucoseValues + ketonesValues
                if (allValues.isNotEmpty()) {
                    val minValue = allValues.minOrNull() ?: 0f
                    val maxValue = allValues.maxOrNull() ?: 0f
                    val padding = when {
                        (maxValue - minValue) < 2 -> 0.5f // Малый диапазон
                        (maxValue - minValue) < 5 -> 1f
                        else -> (maxValue - minValue) * 0.1f // 10% для больших диапазонов
                    }

                    axisMinimum = maxOf(0f, minValue - padding) // Не ниже 0
                    axisMaximum = maxValue + padding
                    granularity = 0.5f // Шаг 0.5 ммоль/л
                }

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "%.1f".format(value) // 1 знак после запятой
                    }
                }
            }

            data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun setupStepsChart(records: List<HealthRecord>) {
        val stepsEntries = mutableListOf<Entry>()
        val dates = mutableListOf<String>()
        val stepsValues = mutableListOf<Float>()
        val textColor = if (isDarkTheme()) Color.WHITE else Color.BLACK

        records.forEachIndexed { index, record ->
            record.steps?.let { steps ->
                stepsEntries.add(Entry(index.toFloat(), steps.toFloat()))
                stepsValues.add(steps.toFloat())
                dates.add(dateFormat.format(record.date))
            }
        }

        val stepsDataSet = LineDataSet(stepsEntries, "Шаги").apply {
            color = Color.GREEN
            lineWidth = 2f
            setCircleColor(Color.GREEN)
            circleRadius = 4f
            valueTextSize = 10f
            valueTextColor = textColor
            setDrawValues(true)
        }

        val lineData = LineData(stepsDataSet)
        binding.stepsChart.apply {
            applyChartTheme(this)

            // Настройка оси X
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dates.getOrNull(value.toInt()) ?: ""
                }
            }

            // Настройка оси Y для шагов
            axisLeft.apply {
                if (stepsValues.isNotEmpty()) {
                    val minSteps = stepsValues.minOrNull() ?: 0f
                    val maxSteps = stepsValues.maxOrNull() ?: 0f
                    val padding = when {
                        (maxSteps - minSteps) < 2000 -> 500f
                        (maxSteps - minSteps) < 5000 -> 1000f
                        else -> (maxSteps - minSteps) * 0.1f
                    }

                    axisMinimum = maxOf(0f, minSteps - padding) // Не ниже 0
                    axisMaximum = maxSteps + padding
                    granularity = 1000f // Шаг 1000 шагов
                }

                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "%.0f".format(value) // Без десятичных знаков
                    }
                }
            }

            data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun setupWaistChart(records: List<HealthRecord>) {
        val waistEntries = mutableListOf<Entry>()
        val dates = mutableListOf<String>()
        val textColor = if (isDarkTheme()) Color.WHITE else Color.BLACK
        val waistValues = mutableListOf<Float>()

        // Собираем данные и значения талии
        records.forEachIndexed { index, record ->
            record.waistSize?.let { waist ->
                waistEntries.add(Entry(index.toFloat(), waist))
                waistValues.add(waist)
                dates.add(dateFormat.format(record.date))
            }
        }

        // Если нет данных - выходим
        if (waistValues.isEmpty()) return

        val waistDataSet = LineDataSet(waistEntries, "Талия (см)").apply {
            color = Color.MAGENTA
            lineWidth = 2f
            setCircleColor(Color.MAGENTA)
            circleRadius = 4f
            valueTextSize = 10f
            valueTextColor = textColor
            setDrawValues(true)
        }

        val lineData = LineData(waistDataSet)
        binding.waistChart.apply {
            applyChartTheme(this)

            // Настройка оси X
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return dates.getOrNull(value.toInt()) ?: ""
                }
            }

            // Настройка оси Y для талии
            axisLeft.apply {
                // Вычисляем min/max с учетом небольших колебаний
                val minValue = (waistValues.minOrNull() ?: 0f).toInt().toFloat() - 1f
                val maxValue = (waistValues.maxOrNull() ?: 0f).toInt().toFloat() + 1f

                // Устанавливаем границы с запасом
                axisMinimum = minValue
                axisMaximum = maxValue

                // Фиксированный шаг 0.5 см для лучшей читаемости
                granularity = 0.5f
                setGranularityEnabled(true)

                // Форматирование значений (1 знак после запятой)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "%.1f".format(value)
                    }
                }
            }

            data = lineData
            animateX(1000)
            invalidate()
        }
    }

    private fun updateStatsSummary(records: List<HealthRecord>) {
        val glucoseValues = records.mapNotNull { it.glucose }
        val stepsValues = records.mapNotNull { it.steps }
        val waistValues = records.mapNotNull { it.waistSize }

        binding.statsSummary.text = buildString {
            appendLine("Период: ${getPeriodString(records)}")
            appendLine()

            if (glucoseValues.isNotEmpty()) {
                appendLine("Глюкоза:")
                appendLine("Среднее: %.1f ммоль/л".format(glucoseValues.average()))
                appendLine("Макс: %.1f ммоль/л".format(glucoseValues.maxOrNull() ?: 0f))
                appendLine("Мин: %.1f ммоль/л".format(glucoseValues.minOrNull() ?: 0f))
                appendLine()
            }

            if (stepsValues.isNotEmpty()) {
                appendLine("Шаги:")
                appendLine("Всего: %,d".format(stepsValues.sum()))
                appendLine("Среднее: %,d в день".format(stepsValues.average().toInt()))
                appendLine("Макс: %,d".format(stepsValues.maxOrNull() ?: 0))
                appendLine("Мин: %,d".format(stepsValues.minOrNull() ?: 0))
                appendLine()
            }

            if (waistValues.isNotEmpty()) {
                appendLine("Талия:")
                appendLine("Среднее: %.1f см".format(waistValues.average()))
                appendLine("Макс: %.1f см".format(waistValues.maxOrNull() ?: 0f))
                appendLine("Мин: %.1f см".format(waistValues.minOrNull() ?: 0f))
            }
        }
    }

    private fun getPeriodString(records: List<HealthRecord>): String {
        if (records.isEmpty()) return "Нет данных"
        val firstDate = records.first().date
        val lastDate = records.last().date
        return "${weekFormat.format(firstDate)} - ${weekFormat.format(lastDate)}"
    }
}