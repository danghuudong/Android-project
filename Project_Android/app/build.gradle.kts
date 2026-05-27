import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun buildConfigString(value: String): String {
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.example.project_android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.project_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "MAIL_USERNAME",
            buildConfigString(localProperties.getProperty("MAIL_USERNAME", "duykhuong29k@gmail.com"))
        )
        buildConfigField(
            "String",
            "MAIL_APP_PASSWORD",
            buildConfigString(localProperties.getProperty("MAIL_APP_PASSWORD", ""))
        )
        buildConfigField(
            "String",
            "REPORT_EXPORT_URL",
            buildConfigString(
                localProperties.getProperty(
                    "REPORT_EXPORT_URL",
                    "http://10.0.2.2:8765/reports/dashboard"
                )
            )
        )
        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            buildConfigString(localProperties.getProperty("GEMINI_API_KEY", ""))
        )
        buildConfigField(
            "String",
            "GEMINI_MODEL",
            buildConfigString(localProperties.getProperty("GEMINI_MODEL", "gemini-2.5-flash"))
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.sun.mail:android-mail:1.6.8")
    implementation("com.sun.mail:android-activation:1.6.8")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
