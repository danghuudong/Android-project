package com.example.project_android.service

import com.example.project_android.network.GeminiClient
import org.json.JSONObject

object AiChatService {

    fun ask(message: String, dashboardData: JSONObject?): String {
        val prompt = buildPrompt(message, dashboardData)
        return GeminiClient.generateText(prompt)
            .getOrElse { fallbackReply(message, dashboardData) }
    }

    private fun buildPrompt(message: String, dashboardData: JSONObject?): String {
        val dashboardContext = dashboardData?.toString(2) ?: "Chua co du lieu dashboard."

        return """
            Ban la chatbot AI trong app quan ly cho thue thoi trang Digital Atelier.
            Nhiem vu cua ban la ho tro admin tra loi ngan gon, thuc te ve:
            - San pham cho thue
            - Don hang
            - Khach hang
            - Doanh thu dashboard
            - Goi y kinh doanh cho cua hang

            Du lieu dashboard hien co:
            $dashboardContext

            Cau hoi cua admin:
            $message

            Quy tac tra loi:
            - Tra loi bang tieng Viet.
            - Giai thich de hieu cho nguoi moi quan ly cua hang.
            - Neu cau hoi lien quan so lieu, hay dua ra nhan xet dua tren dashboard.
            - Neu khong du du lieu, noi ro la can them du lieu, khong bia so lieu.
            - Khong tu y sua/xoa/tao don hang hay san pham.
        """.trimIndent()
    }

    private fun fallbackReply(message: String, dashboardData: JSONObject?): String {
        val revenue = dashboardData?.optDouble("revenue", 0.0) ?: 0.0
        val activeOrders = dashboardData?.optLong("activeOrderCount", 0) ?: 0
        val topProduct = dashboardData
            ?.optJSONArray("topProducts")
            ?.optJSONObject(0)
            ?.optString("name")
            .orEmpty()

        return when {
            message.contains("doanh thu", ignoreCase = true) -> {
                "Gemini dang khong phan hoi nen minh dung che do demo. Doanh thu hien tai tren dashboard la ${formatMoney(revenue)}. Ban nen so sanh voi tang truong va top san pham de quyet dinh co can khuyen mai hay nhap them hang."
            }
            message.contains("san pham", ignoreCase = true) || message.contains("sản phẩm", ignoreCase = true) -> {
                if (topProduct.isNotBlank()) {
                    "Gemini dang khong phan hoi nen minh dung che do demo. San pham dang noi bat la $topProduct. Ban nen kiem tra ton kho, size pho bien va hinh anh san pham nay."
                } else {
                    "Gemini dang khong phan hoi nen minh dung che do demo. Hien chua co san pham noi bat ro rang tren dashboard, can them du lieu thue de phan tich tot hon."
                }
            }
            message.contains("don", ignoreCase = true) || message.contains("đơn", ignoreCase = true) -> {
                "Gemini dang khong phan hoi nen minh dung che do demo. Hien co $activeOrders don dang thue. Ban nen theo doi lich tra hang va nhac khach truoc ngay tra."
            }
            else -> {
                "Gemini dang khong phan hoi nen minh dung che do demo. Ban co the hoi ve doanh thu, don hang, san pham ban chay hoac goi y kinh doanh dua tren dashboard."
            }
        }
    }

    private fun formatMoney(amount: Double): String {
        return "${String.format(java.util.Locale.US, "%,.0f", amount).replace(",", ".")} d"
    }
}
