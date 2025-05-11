package com.kosta.healthtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import com.kosta.healthtracker.data.HealthRecord
import java.util.*

class HealthRecordAdapter(
    private var records: List<HealthRecord>,
    private val onItemClick: (HealthRecord) -> Unit
) : RecyclerView.Adapter<HealthRecordAdapter.ViewHolder>() {

    // ViewHolder для элементов списка
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val imgGlucoseKetones: ImageView = itemView.findViewById(R.id.imgGlucoseKetones)
        val tvGlucoseKetones: TextView = itemView.findViewById(R.id.tvGlucoseKetones)
        val tvSteps: TextView = itemView.findViewById(R.id.tvSteps)
        val imgMeals: ImageView = itemView.findViewById(R.id.imgMeals)
        val tvMeals: TextView = itemView.findViewById(R.id.tvMeals)
        val imgComment: ImageView = itemView.findViewById(R.id.imgComment)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        val tvWaistSize: TextView = itemView.findViewById(R.id.tvWaistSize)
        val imgActivity: ImageView = itemView.findViewById(R.id.imgActivity)
    }

    // Обновление списка записей
    fun updateRecords(newRecords: List<HealthRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return ViewHolder(view)
    }
    private fun bindOptionalText(
        textView: TextView,
        prefix: String,
        value: Any?,
        unit: String = ""
    ) {
        textView.apply {
            text = value?.let { "$prefix: $it$unit" } ?: ""
            visibility = if (value != null) View.VISIBLE else View.GONE
        }
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        // Форматирование даты
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(record.date)

        // Глюкоза и кетоны
        val glucoseKetones = listOfNotNull(
            record.glucose?.let { "Глк: $it ммоль/л" },
            record.ketones?.let { "Кт: $it ммоль/л" }
        )
        val hasGlucoseKetones = glucoseKetones.isNotEmpty()  // Флаг для проверки наличия данных
        holder.tvGlucoseKetones.text = glucoseKetones.joinToString(", ").ifEmpty { "—" }
        // Управление видимостью иконки
        holder.imgGlucoseKetones.visibility = if (hasGlucoseKetones) View.VISIBLE else View.GONE

        // Размер талии
        bindOptionalText(holder.tvWaistSize, "Талия", record.waistSize, " см, ")
        // Шаги
        bindOptionalText(holder.tvSteps, "Шаги", record.steps)
        val hasActivityData = record.waistSize != null || record.steps != null
        holder.imgActivity.visibility = if (hasActivityData) View.VISIBLE else View.GONE

        // Приемы пищи (особый случай с joinToString)
        val meals = listOfNotNull(
            record.breakfastTime?.let { "$it" },
            record.lunchTime?.let { "$it" },
            record.dinnerTime?.let { "$it" }
        )
        val hasMeals = meals.isNotEmpty()  // Флаг для проверки наличия данных
        holder.tvMeals.apply {
            text = meals.joinToString(" — ")
            visibility = if (meals.isNotEmpty()) View.VISIBLE else View.GONE
        }
        // Управление видимостью иконки
        holder.imgMeals.visibility = if (hasMeals) View.VISIBLE else View.GONE


        // Комментарий
        holder.tvComment.apply {
            text = record.comment ?: ""
            visibility = if (record.comment.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        val hasComment = record.comment != null
        holder.imgComment.visibility = if (hasComment) View.VISIBLE else View.GONE

        // Обработка клика
        holder.itemView.setOnClickListener { onItemClick(record) }
    }


    override fun getItemCount() = records.size
}