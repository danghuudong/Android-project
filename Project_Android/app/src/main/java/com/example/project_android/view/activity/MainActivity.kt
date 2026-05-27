package com.example.project_android.view.activity

import com.example.project_android.R

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.project_android.navigation.AppNavigator
import com.example.project_android.network.ApiClient
import com.example.project_android.network.runApi
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var edtIdentifier: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var cbRemember: CheckBox
    private lateinit var btnLogin: View

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        restoreRememberedCredentials()
        setupEvents()
    }

    private fun initViews() {
        edtIdentifier = findViewById(R.id.edtIdentifier)
        edtPassword = findViewById(R.id.edtPassword)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
        cbRemember = findViewById(R.id.cbRemember)
        btnLogin = findViewById(R.id.btnLogin)
    }

    private fun setupEvents() {
        btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        btnLogin.setOnClickListener { handleLogin() }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        edtPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        edtPassword.setSelection(edtPassword.text.length)
    }

    private fun handleLogin() {
        val identifier = edtIdentifier.text.toString().trim()
        val password = edtPassword.text.toString()

        if (identifier.isEmpty()) {
            edtIdentifier.error = "Nhập email hoặc số điện thoại"
            edtIdentifier.requestFocus()
            return
        }

        if (password.isEmpty()) {
            edtPassword.error = "Nhập mật khẩu"
            edtPassword.requestFocus()
            return
        }

        val requestBody = JSONObject().apply {
            put("identifier", identifier)
            put("password", password)
            put("remember", cbRemember.isChecked)
        }

        // Demo flow: app kiểm tra tài khoản admin cấp sẵn -> đúng thì vào dashboard.
        runApi(
            loadingMessage = "Đang đăng nhập...",
            request = { ApiClient.post("/auth/login", requestBody) },
            failureMessage = { result -> result.body }
        ) {
            saveRememberedCredentials(identifier, password)
            Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
            AppNavigator.openDashboard(this)
            finish()
        }
    }

    /**
     * Khi user đã chọn ghi nhớ, điền lại thông tin vào form ở lần mở app tiếp theo.
     * Đây là hành vi phục vụ demo; production không nên lưu mật khẩu dạng rõ.
     */
    private fun restoreRememberedCredentials() {
        val preferences = getSharedPreferences(PREF_LOGIN, MODE_PRIVATE)
        val isRemembered = preferences.getBoolean(KEY_REMEMBER, false)

        if (!isRemembered) {
            return
        }

        cbRemember.isChecked = true
        edtIdentifier.setText(preferences.getString(KEY_EMAIL, "").orEmpty())
        edtPassword.setText(preferences.getString(KEY_PASSWORD, "").orEmpty())
    }

    /**
     * Chỉ lưu email/mật khẩu khi checkbox được chọn và login đã thành công.
     * Nếu admin bỏ chọn, thông tin cũ sẽ bị xoá khỏi bộ nhớ ứng dụng.
     */
    private fun saveRememberedCredentials(identifier: String, password: String) {
        val editor = getSharedPreferences(PREF_LOGIN, MODE_PRIVATE).edit()

        if (cbRemember.isChecked) {
            editor
                .putBoolean(KEY_REMEMBER, true)
                .putString(KEY_EMAIL, identifier)
                .putString(KEY_PASSWORD, password)
        } else {
            editor.clear()
        }

        editor.apply()
    }

    companion object {
        private const val PREF_LOGIN = "login_preferences"
        private const val KEY_REMEMBER = "remember_account"
        private const val KEY_EMAIL = "admin_email"
        private const val KEY_PASSWORD = "admin_password"
    }
}

