import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.bh.beanie"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bh.beanie"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val localProperties = rootProject.file("local.properties")
        if (localProperties.exists()) {
            val properties = Properties().apply {
                load(localProperties.inputStream())
            }

            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${properties["CLOUDINARY_CLOUD_NAME"]}\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"${properties["CLOUDINARY_API_KEY"]}\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${properties["CLOUDINARY_API_SECRET"]}\"")
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.mpandroidchart)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.recyclerview)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation (libs.google.play.services.auth)
    implementation (libs.kotlinx.coroutines.play.services)


    implementation(libs.firebase.firestore)
    // implementation(libs.firebase.storage)  // - dung thi bo comment
    // implementation(libs.firebase.messaging)

    implementation(libs.cloudinary.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.zxing.core)
    implementation(libs.zxing.embedded)
}