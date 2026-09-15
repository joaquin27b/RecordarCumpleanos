plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.blair.cumpleanos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.blair.cumpleanos"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // IDs de PRUEBA de Google: el unico build que se instala en el telefono.
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            manifestPlaceholders["adAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            // Ad units propias de com.blair.cumpleanos.
            // Un clic propio con estos IDs suspende la cuenta AdMob entera.
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-6760097434219921/3170801198\"")
            manifestPlaceholders["adAppId"] = "ca-app-pub-6760097434219921~8061744806"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // play-services-ads arrastra fragment 1.1.0 (2019), que rompe el flujo
    // de registerForActivityResult: FragmentActivity no llamaba
    // super.onRequestPermissionsResult y usaba request codes invalidos.
    implementation(libs.androidx.fragment)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
