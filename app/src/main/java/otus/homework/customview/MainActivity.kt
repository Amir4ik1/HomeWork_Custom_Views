package otus.homework.customview

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.customview.databinding.ActivityMainBinding
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val payloads = parsePayloadFromFile(this, R.raw.payload)
        binding.pieChartView.setPayloads(payloads)
        binding.pieChartView.setCenterText(
            text = "₽${payloads.sumOf { it.amount }}",
            subText = "за месяц"
        )
        binding.pieChartView.setOnCategoryClickListener { category ->
            Toast.makeText(this, category, Toast.LENGTH_SHORT).show()
        }

        val categories = listOf("Продукты", "Здоровье", "Транспорт")
        val palette = listOf(
            "#FF6C4A".toColorInt(),
            "#4682B4".toColorInt(),
            "#43A047".toColorInt()
        )
        val seriesList = categories.mapIndexed { idx, cat ->
            val data = payloads.filter { it.category == cat }
                .groupBy { it.time / 86400 }
                .map { (day, items) -> LineChartView.Point(day.toInt(), items.sumOf { it.amount }.toFloat()) }
            LineChartView.Series(cat, palette[idx % palette.size], data)
        }
        binding.lineChartView.setData(seriesList)

    }

    private fun parsePayloadFromFile(context: Context, rawResId: Int): List<PayloadModel> {
        context.resources.openRawResource(rawResId).use { inputStream ->
            val json = inputStream.bufferedReader().readText()
            val type = object : TypeToken<List<PayloadModel>>() {}.type
            return Gson().fromJson(json, type)
        }
    }
}