package com.example.project_android.view.activity

import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.project_android.R
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.navigation.SidebarController
import com.example.project_android.network.ApiClient
import com.example.project_android.network.DashboardReportUploader
import com.example.project_android.network.runApi
import com.example.project_android.utils.DashboardExcelExporter
import com.example.project_android.utils.ImageUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnMenu: ImageView
    private lateinit var btnDrawerMenu: ImageView
    private lateinit var menuOverview: LinearLayout
    private lateinit var menuCategoryManagement: LinearLayout

    private lateinit var btnExportReport: LinearLayout
    private lateinit var navHome: LinearLayout
    private lateinit var navOrders: LinearLayout
    private lateinit var navProducts: LinearLayout
    private lateinit var navCustomers: LinearLayout

    private lateinit var txtTotalRevenue: TextView
    private lateinit var txtRevenueGrowth: TextView
    private lateinit var txtActiveOrders: TextView
    private lateinit var txtTotalOrders: TextView
    private lateinit var txtRentedProducts: TextView
    private lateinit var chartBars: List<View>
    private lateinit var chartLabels: List<TextView>
    private lateinit var topProductViews: List<TopProductViews>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initViews()
        showLoadingState()
        setupEvents()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardSummary()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenu = findViewById(R.id.btnMenu)
        btnDrawerMenu = findViewById(R.id.btnDrawerMenu)
        menuOverview = findViewById(R.id.menuOverview)
        menuCategoryManagement = findViewById(R.id.menuCategoryManagement)
        btnExportReport = findViewById(R.id.btnExportReport)
        navHome = findViewById(R.id.navHome)
        navOrders = findViewById(R.id.navOrders)
        navProducts = findViewById(R.id.navProducts)
        navCustomers = findViewById(R.id.navCustomers)

        txtTotalRevenue = findViewById(R.id.txtTotalRevenue)
        txtRevenueGrowth = findViewById(R.id.txtRevenueGrowth)
        txtActiveOrders = findViewById(R.id.txtActiveOrders)
        txtTotalOrders = findViewById(R.id.txtTotalOrders)
        txtRentedProducts = findViewById(R.id.txtRentedProducts)
        chartBars = listOf(
            findViewById(R.id.chartBar1),
            findViewById(R.id.chartBar2),
            findViewById(R.id.chartBar3),
            findViewById(R.id.chartBar4),
            findViewById(R.id.chartBar5),
            findViewById(R.id.chartBar6),
            findViewById(R.id.chartBar7)
        )
        chartLabels = listOf(
            findViewById(R.id.txtChartLabel1),
            findViewById(R.id.txtChartLabel2),
            findViewById(R.id.txtChartLabel3),
            findViewById(R.id.txtChartLabel4),
            findViewById(R.id.txtChartLabel5),
            findViewById(R.id.txtChartLabel6),
            findViewById(R.id.txtChartLabel7)
        )
        topProductViews = listOf(
            topProductViews(
                R.id.itemTop1, R.id.imgTop1, R.id.txtTop1Code, R.id.txtTop1Name,
                R.id.txtTop1Category, R.id.txtTop1Rentals, R.id.txtTop1Revenue
            ),
            topProductViews(
                R.id.itemTop2, R.id.imgTop2, R.id.txtTop2Code, R.id.txtTop2Name,
                R.id.txtTop2Category, R.id.txtTop2Rentals, R.id.txtTop2Revenue
            ),
            topProductViews(
                R.id.itemTop3, R.id.imgTop3, R.id.txtTop3Code, R.id.txtTop3Name,
                R.id.txtTop3Category, R.id.txtTop3Rentals, R.id.txtTop3Revenue
            )
        )
    }

    private fun showLoadingState() {
        txtTotalRevenue.text = "--"
        txtRevenueGrowth.text = "--"
        txtActiveOrders.text = "--"
        txtTotalOrders.text = "--"
        txtRentedProducts.text = "--"
        renderChart(JSONArray())
        topProductViews.forEach { it.card.visibility = View.GONE }
    }

    private fun setupEvents() {
        SidebarController.bindFromActivity(this)

        btnExportReport.setOnClickListener { prepareExcelExport() }
        navHome.setOnClickListener { loadDashboardSummary(showToast = true) }
        navOrders.setOnClickListener { AppNavigator.openOrders(this) }
        navProducts.setOnClickListener { AppNavigator.openProducts(this) }
        navCustomers.setOnClickListener { AppNavigator.openCustomers(this) }
    }

    private fun loadDashboardSummary(showToast: Boolean = false) {
        runApi(
            loadingMessage = if (showToast) "\u0110ang t\u1ea3i dashboard..." else null,
            request = { ApiClient.get("/reports/dashboard") }
        ) { result ->
            renderDashboard(JSONObject(result.body).getJSONObject("data"))
            if (showToast) {
                Toast.makeText(this, "\u0110\u00e3 c\u1eadp nh\u1eadt dashboard", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareExcelExport() {
        runApi(
            loadingMessage = "\u0110ang chu\u1ea9n b\u1ecb file Excel...",
            request = { ApiClient.get("/reports/dashboard") }
        ) { result ->
            val data = JSONObject(result.body).getJSONObject("data")
            renderDashboard(data)
            exportToComputer(data)
        }
    }

    private fun exportToComputer(data: JSONObject) {
        val fileName = reportFileName()
        Thread {
            val reportBytes = DashboardExcelExporter.createBytes(data)
            val result = DashboardReportUploader.upload(fileName, reportBytes)
            runOnUiThread {
                result.onSuccess {
                    Toast.makeText(
                        this,
                        "\u0110\u00e3 xu\u1ea5t file v\u00e0o th\u01b0 m\u1ee5c reports/dashboard tr\u00ean m\u00e1y t\u00ednh",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure {
                    Toast.makeText(
                        this,
                        "Kh\u00f4ng xu\u1ea5t \u0111\u01b0\u1ee3c. H\u00e3y ch\u1ea1y tools\\Start-ReportReceiver.ps1 tr\u00ean m\u00e1y t\u00ednh.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun reportFileName(): String {
        val date = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "Bao_Cao_Tong_Quan_$date.xlsx"
    }

    private fun renderDashboard(data: JSONObject) {
        txtTotalRevenue.text = formatMoney(data.optDouble("revenue", 0.0))
        renderGrowth(data.optDouble("growthPercent", 0.0))
        txtActiveOrders.text = formatCount(data.optLong("activeOrderCount", 0))
        txtTotalOrders.text = formatCount(data.optLong("orderCount", 0))
        txtRentedProducts.text = formatCount(data.optLong("rentedProductCount", 0))
        renderChart(data.optJSONArray("revenueSeries") ?: JSONArray())
        renderTopProducts(data.optJSONArray("topProducts") ?: JSONArray())
    }

    private fun renderGrowth(growthPercent: Double) {
        val sign = if (growthPercent >= 0) "+" else ""
        txtRevenueGrowth.text = "$sign${String.format(Locale.US, "%.1f", growthPercent)} %"
        val positive = growthPercent >= 0
        txtRevenueGrowth.setBackgroundResource(
            if (positive) R.drawable.bg_status_ready else R.drawable.bg_discount_status_expired
        )
        txtRevenueGrowth.setTextColor(
            getColor(if (positive) R.color.brand_primary else R.color.discount_error_text)
        )
    }

    private fun renderChart(series: JSONArray) {
        val amounts = chartBars.indices.map { index ->
            val item = series.optJSONObject(index)
            item?.optDouble("amount", 0.0) ?: series.optDouble(index, 0.0)
        }
        val maxAmount = amounts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val currentMonthIndex = chartBars.indices.firstOrNull { index ->
            series.optJSONObject(index)?.optBoolean("isCurrentMonth", false) == true
        } ?: -1

        chartBars.forEachIndexed { index, bar ->
            val height = 12 + ((amounts[index] / maxAmount) * 124).roundToInt()
            bar.layoutParams = bar.layoutParams.apply { this.height = dp(height) }
            bar.setBackgroundResource(
                if (index == currentMonthIndex) {
                    R.drawable.bg_chart_bar_primary
                } else {
                    R.drawable.bg_chart_bar_soft
                }
            )
        }
        chartLabels.forEachIndexed { index, label ->
            label.text = series.optJSONObject(index)?.optString("label", "-") ?: "-"
            val isCurrentMonth = index == currentMonthIndex
            label.setTypeface(label.typeface, if (isCurrentMonth) Typeface.BOLD else Typeface.NORMAL)
            label.setTextColor(getColor(if (isCurrentMonth) R.color.brand_primary else R.color.text_secondary))
        }
    }

    private fun renderTopProducts(products: JSONArray) {
        topProductViews.forEachIndexed { index, views ->
            val product = products.optJSONObject(index)
            if (product == null) {
                views.card.visibility = View.GONE
                return@forEachIndexed
            }

            views.card.visibility = View.VISIBLE
            views.code.text = product.optString("id", "--")
            views.name.text = product.optString("name", "--")
            views.category.text = "${product.optString("category", "--")} | Size ${product.optString("size", "--")}"
            views.rentals.text = "${formatCount(product.optLong("rentalCount", 0))} l\u01b0\u1ee3t thu\u00ea"
            views.revenue.text = formatMoney(product.optDouble("generatedRevenue", 0.0))
            ImageUtils.bindImage(views.image, product.optString("imageUrl", ""), android.R.drawable.ic_menu_gallery)

            val productId = product.optString("id", "")
            views.card.setOnClickListener {
                if (productId.isNotBlank()) AppNavigator.openProductDetail(this, productId)
            }
        }
    }

    private fun formatMoney(amount: Double): String {
        return "${String.format(Locale.US, "%,.0f", amount).replace(",", ".")} \u0111"
    }

    private fun formatCount(value: Long): String {
        return String.format(Locale.US, "%,d", value).replace(",", ".")
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private fun topProductViews(
        cardId: Int,
        imageId: Int,
        codeId: Int,
        nameId: Int,
        categoryId: Int,
        rentalsId: Int,
        revenueId: Int
    ): TopProductViews {
        return TopProductViews(
            card = findViewById(cardId),
            image = findViewById(imageId),
            code = findViewById(codeId),
            name = findViewById(nameId),
            category = findViewById(categoryId),
            rentals = findViewById(rentalsId),
            revenue = findViewById(revenueId)
        )
    }

    private data class TopProductViews(
        val card: LinearLayout,
        val image: ImageView,
        val code: TextView,
        val name: TextView,
        val category: TextView,
        val rentals: TextView,
        val revenue: TextView
    )

}
