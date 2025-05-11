package com.kosta.healthtracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import com.kosta.healthtracker.databinding.ActivityMainBinding
import com.kosta.healthtracker.R
import com.kosta.healthtracker.data.AppDatabase
import com.kosta.healthtracker.data.HealthRecordRepository
import com.kosta.healthtracker.viewmodel.HealthRecordViewModel
import com.kosta.healthtracker.viewmodel.HealthRecordViewModelFactory
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn // Добавьте этот импорт
import kotlinx.coroutines.flow.onEach
import android.app.ActivityOptions
import android.view.Menu
import android.view.MenuItem


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: HealthRecordViewModel
    private lateinit var adapter: HealthRecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel и базы данных
        val dao = AppDatabase.getInstance(application).healthRecordDao()
        viewModel = ViewModelProvider(
            this,
            HealthRecordViewModelFactory(HealthRecordRepository(dao))
        )[HealthRecordViewModel::class.java]

        // Настройка RecyclerView
        adapter = HealthRecordAdapter(emptyList()) { record ->
            // Открытие экрана редактирования при клике
            val intent = Intent(this, EditRecordActivity::class.java).apply {
                putExtra("record_id", record.id)
            }
            startActivity(intent)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            //startActivity(intent, options.toBundle())
        }

        binding.rvRecords.layoutManager = LinearLayoutManager(this)
        binding.rvRecords.adapter = adapter

        // Кнопка добавления записи
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, EditRecordActivity::class.java)
            startActivity(intent)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            //startActivity(intent, options.toBundle())
        }

        // Подписка на изменения данных
        viewModel.allRecords.onEach { records ->
            adapter.updateRecords(records)
            binding.tvNoRecords.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }.launchIn(lifecycleScope)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import_export -> {
                startActivity(Intent(this, ImportExportActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}