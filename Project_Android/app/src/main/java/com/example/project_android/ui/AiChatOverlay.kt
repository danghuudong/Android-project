package com.example.project_android.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.project_android.R
import com.example.project_android.network.ApiClient
import com.example.project_android.service.AiChatService
import org.json.JSONObject
import kotlin.math.abs

object AiChatOverlay {
    private const val OVERLAY_TAG = "global_ai_chat_overlay"
    private val allowedActivities = setOf(
        "DashboardActivity",
        "ProductActivity",
        "OrderActivity",
        "CustomerActivity",
        "CategoryManagementActivity",
        "DiscountCodeActivity"
    )

    fun bind(activity: Activity) {
        if (activity.javaClass.simpleName !in allowedActivities) return

        val contentRoot = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (contentRoot.findViewWithTag<View>(OVERLAY_TAG) != null) return

        val overlay = ChatOverlayView(activity)
        overlay.tag = OVERLAY_TAG
        contentRoot.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
    }

    private class ChatOverlayView(private val activity: Activity) : FrameLayout(activity) {
        private val iconButton: LinearLayout
        private val chatPanel: LinearLayout
        private val chatHeader: LinearLayout
        private val txtMessages: TextView
        private val scrollMessages: ScrollView
        private val edtMessage: EditText
        private val btnSend: View
        private val chatHistory = StringBuilder(
            "AI: Xin chào, tôi có thể hỗ trợ bạn phân tích doanh thu, đơn hàng, sản phẩm cho thuê và gợi ý kinh doanh."
        )

        init {
            isClickable = false
            isFocusable = false

            chatPanel = createChatPanel()
            iconButton = createIconButton()

            addView(chatPanel)
            addView(iconButton)

            txtMessages = chatPanel.findViewWithTag("txtMessages")
            scrollMessages = chatPanel.findViewWithTag("scrollMessages")
            edtMessage = chatPanel.findViewWithTag("edtMessage")
            btnSend = chatPanel.findViewWithTag("btnSend")
            chatHeader = chatPanel.findViewWithTag("chatHeader")

            setupEvents()
        }

        private fun createChatPanel(): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
                alpha = 0f
                scaleX = 0.82f
                scaleY = 0.82f
                setBackgroundResource(R.drawable.bg_form_card)
                elevation = dp(12).toFloat()
                setPadding(dp(14), dp(14), dp(14), dp(14))

                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    dp(386),
                    Gravity.BOTTOM
                ).apply {
                    leftMargin = dp(14)
                    rightMargin = dp(14)
                    bottomMargin = dp(98)
                }

                addView(createHeader())
                addView(createMessageScroll())
                addView(createInputRow())
            }
        }

        private fun createHeader(): LinearLayout {
            return LinearLayout(activity).apply {
                tag = "chatHeader"
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL

                addView(ImageView(activity).apply {
                    setImageResource(R.drawable.ic_chatbot_spark)
                    // Icon vector co phan duoi rong hon mot chut, day xuong nhe de thang hang voi chu.
                    translationY = dp(2).toFloat()
                }, LinearLayout.LayoutParams(dp(26), dp(26)))

                addView(TextView(activity).apply {
                    text = "Chatbot AI"
                    setTextColor(activity.getColor(R.color.text_primary))
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = dp(8)
                })

                addView(TextView(activity).apply {
                    text = "×"
                    gravity = Gravity.CENTER
                    setTextColor(Color.parseColor("#4F46E5"))
                    textSize = 22f
                    setTypeface(typeface, Typeface.BOLD)
                    setOnClickListener { hidePanel() }
                }, LinearLayout.LayoutParams(dp(36), dp(36)))
            }
        }

        private fun createMessageScroll(): ScrollView {
            return ScrollView(activity).apply {
                tag = "scrollMessages"
                isFillViewport = true
                addView(TextView(activity).apply {
                    tag = "txtMessages"
                    text = chatHistory.toString()
                    setTextColor(Color.parseColor("#3F3A53"))
                    textSize = 15f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setLineSpacing(dp(6).toFloat(), 1.04f)
                })
                layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0,
                    1f
                ).apply {
                    topMargin = dp(10)
                }
            }
        }

        private fun createInputRow(): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    dp(46)
                ).apply {
                    topMargin = dp(10)
                }

                addView(EditText(activity).apply {
                    tag = "edtMessage"
                    // Nen input chi nam o phan nhap text, khong tran sang phan nut gui.
                    background = inputLeftDrawable()
                    hint = "Nhập câu hỏi..."
                    imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NONE
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    maxLines = 2
                    includeFontPadding = true
                    // Padding phai giup chu khong sat vao nut gui khi go dai.
                    setPadding(dp(12), 0, dp(12), 0)
                    setTextColor(activity.getColor(R.color.text_primary))
                    setHintTextColor(activity.getColor(R.color.text_label))
                    textSize = 14f
                }, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))

                addView(SendButtonView(activity).apply {
                    tag = "btnSend"
                }, LinearLayout.LayoutParams(dp(46), LayoutParams.MATCH_PARENT))
            }
        }

        private fun createIconButton(): LinearLayout {
            return LinearLayout(activity).apply {
                gravity = Gravity.CENTER
                background = circleDrawable(Color.parseColor("#1D4ED8"))
                elevation = dp(6).toFloat()
                isClickable = true

                addView(ImageView(activity).apply {
                    setImageResource(R.drawable.ic_chatbot_spark)
                    translationY = dp(2).toFloat()
                }, LinearLayout.LayoutParams(dp(40), dp(40)))

                layoutParams = LayoutParams(dp(62), dp(62), Gravity.BOTTOM or Gravity.END).apply {
                    rightMargin = dp(18)
                    bottomMargin = dp(104)
                }
            }
        }

        private fun setupEvents() {
            var downRawX = 0f
            var downRawY = 0f
            var startX = 0f
            var startY = 0f
            var moved = false

            iconButton.setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = view.x
                        startY = view.y
                        moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        if (abs(dx) > dp(4) || abs(dy) > dp(4)) moved = true
                        moveIcon(startX + dx, startY + dy)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) togglePanel()
                        true
                    }
                    else -> false
                }
            }

            chatHeader.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = event.rawX
                        downRawY = event.rawY
                        startX = chatPanel.x
                        startY = chatPanel.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        movePanel(startX + dx, startY + dy)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> true
                    else -> false
                }
            }

            btnSend.setOnClickListener { sendMessage() }
        }

        private fun moveIcon(targetX: Float, targetY: Float) {
            val maxX = (width - iconButton.width).toFloat().coerceAtLeast(0f)
            val maxY = (height - iconButton.height).toFloat().coerceAtLeast(0f)
            iconButton.x = targetX.coerceIn(0f, maxX)
            iconButton.y = targetY.coerceIn(0f, maxY)
            updatePanelPivot()
        }

        private fun movePanel(targetX: Float, targetY: Float) {
            val maxX = (width - chatPanel.width).toFloat().coerceAtLeast(0f)
            val maxY = (height - chatPanel.height).toFloat().coerceAtLeast(0f)
            chatPanel.x = targetX.coerceIn(0f, maxX)
            chatPanel.y = targetY.coerceIn(0f, maxY)
            updatePanelPivot()
        }

        private fun togglePanel() {
            if (chatPanel.visibility == View.VISIBLE) {
                hidePanel()
            } else {
                showPanel()
            }
        }

        private fun showPanel() {
            updatePanelPivot()
            iconButton.visibility = View.GONE
            chatPanel.visibility = View.VISIBLE
            chatPanel.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .start()
        }

        private fun hidePanel() {
            updatePanelPivot()
            chatPanel.animate()
                .alpha(0f)
                .scaleX(0.82f)
                .scaleY(0.82f)
                .setDuration(150)
                .withEndAction {
                    chatPanel.visibility = View.GONE
                    iconButton.visibility = View.VISIBLE
                }
                .start()
        }

        private fun updatePanelPivot() {
            chatPanel.post {
                val iconCenterX = iconButton.x + iconButton.width / 2f
                val iconCenterY = iconButton.y + iconButton.height / 2f
                chatPanel.pivotX = (iconCenterX - chatPanel.x).coerceIn(0f, chatPanel.width.toFloat())
                chatPanel.pivotY = (iconCenterY - chatPanel.y).coerceIn(0f, chatPanel.height.toFloat())
            }
        }

        private fun sendMessage() {
            val message = edtMessage.text.toString().trim()
            if (message.isBlank()) return

            edtMessage.setText("")
            appendMessage("Bạn", message)
            appendMessage("AI", "Đang trả lời...")
            setSending(true)

            Thread {
                val dashboardData = loadDashboardData()
                val reply = AiChatService.ask(message, dashboardData)
                activity.runOnUiThread {
                    removeLastMessage("AI\nĐang trả lời...")
                    appendMessage("AI", reply)
                    setSending(false)
                }
            }.start()
        }

        private fun loadDashboardData(): JSONObject? {
            return runCatching {
                ApiClient.init(activity)
                val result = ApiClient.get("/reports/dashboard")
                if (result.isSuccess) JSONObject(result.body).getJSONObject("data") else null
            }.getOrNull()
        }

        private fun appendMessage(sender: String, message: String) {
            if (chatHistory.isNotEmpty()) chatHistory.append("\n\n")
            chatHistory.append(sender)
                .append("\n")
                .append(formatChatContent(message))
            txtMessages.text = chatHistory.toString()
            scrollMessages.post { scrollMessages.fullScroll(View.FOCUS_DOWN) }
        }

        private fun formatChatContent(message: String): String {
            return message
                .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
                .replace(Regex("""__(.*?)__"""), "$1")
                .replace(Regex("""`([^`]*)`"""), "$1")
                .lines()
                .joinToString("\n") { line ->
                    line
                        .replace(Regex("""^\s*[-*]\s+"""), "• ")
                        .replace(Regex("""^\s*(\d+)[.)]\s+"""), "$1. ")
                        .trimEnd()
                }
                .trim()
        }

        private fun removeLastMessage(message: String) {
            val suffix = "\n\n$message"
            when {
                chatHistory.toString() == message -> chatHistory.clear()
                chatHistory.endsWith(suffix) -> chatHistory.delete(chatHistory.length - suffix.length, chatHistory.length)
            }
            txtMessages.text = chatHistory.toString()
        }

        private fun setSending(isSending: Boolean) {
            btnSend.isEnabled = !isSending
            btnSend.alpha = if (isSending) 0.6f else 1f
        }

        private fun dp(value: Int): Int {
            return (value * resources.displayMetrics.density).toInt()
        }

        private fun roundedDrawable(color: Int, radius: Float): GradientDrawable {
            return GradientDrawable().apply {
                setColor(color)
                cornerRadius = radius
            }
        }

        private fun circleDrawable(color: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }

        private fun inputLeftDrawable(): GradientDrawable {
            return GradientDrawable().apply {
                setColor(activity.getColor(R.color.input_background))
                cornerRadii = floatArrayOf(
                    dp(8).toFloat(), dp(8).toFloat(), // top-left
                    0f, 0f,                           // top-right
                    0f, 0f,                           // bottom-right
                    dp(8).toFloat(), dp(8).toFloat()  // bottom-left
                )
            }
        }

        private class SendButtonView(context: android.content.Context) : View(context) {
            private val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1D4ED8")
                style = android.graphics.Paint.Style.FILL
            }
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = android.graphics.Paint.Style.FILL
            }

            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                val w = width.toFloat()
                val h = height.toFloat()
                val backgroundPath = Path().apply {
                    addRoundRect(
                        android.graphics.RectF(0f, 0f, w, h),
                        floatArrayOf(
                            0f, 0f,                 // top-left
                            h * 0.16f, h * 0.16f,   // top-right
                            h * 0.16f, h * 0.16f,   // bottom-right
                            0f, 0f                  // bottom-left
                        ),
                        Path.Direction.CW
                    )
                }
                canvas.drawPath(backgroundPath, bgPaint)
                val path = Path().apply {
                    moveTo(w * 0.34f, h * 0.30f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.34f, h * 0.70f)
                    lineTo(w * 0.40f, h * 0.55f)
                    lineTo(w * 0.56f, h * 0.50f)
                    lineTo(w * 0.40f, h * 0.45f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
        }
    }
}
