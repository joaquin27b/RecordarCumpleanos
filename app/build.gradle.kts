import java.util.Properties

val propsFirma = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

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
        versionCode = 3
        versionName = "1.1.1"
        buildConfigField("boolean", "PEDIR_ANUNCIO", "true")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // keystore.properties esta en .gitignore: la clave NUNCA al repo.
            propsFirma.getProperty("storeFile")?.let { storeFile = file(it) }
            storePassword = propsFirma.getProperty("storePassword")
            keyAlias = propsFirma.getProperty("keyAlias")
            keyPassword = propsFirma.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Paquete propio: convive con la de Play en vez de chocar con
            // INSTALL_FAILED_UPDATE_INCOMPATIBLE (claves de firma distintas).
            // Ademas run-as solo funciona sobre builds debuggable.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // IDs de PRUEBA de Google: el unico build que se instala en el telefono.
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            manifestPlaceholders["adAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
        release {
            // Ad units propias de com.blair.cumpleanos.
            // Un clic propio con estos IDs suspende la cuenta AdMob entera.
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-6760097434219921/3170801198\"")
            manifestPlaceholders["adAppId"] = "ca-app-pub-6760097434219921~8061744806"
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Release con R8 y no depurable, para medir fluidez y sacar capturas con
        // datos de prueba. Paquete propio: no pisa la release instalada. IDs de
        // anuncio de PRUEBA: impresiones propias con los reales son trafico invalido.
        create("medicion") {
            initWith(getByName("release"))
            applicationIdSuffix = ".medicion"
            versionNameSuffix = "-medicion"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            // Capturas de la ficha: la franja queda, vacia; "Anuncio de prueba" no.
            buildConfigField("boolean", "PEDIR_ANUNCIO", "false")
            buildConfigField("String", "AD_UNIT_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            manifestPlaceholders["adAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
    }
    // El sembrador de datos de prueba existe en debug y medicion, nunca en release.
    sourceSets {
        getByName("debug").kotlin.srcDir("src/sembrador/java")
        getByName("medicion").kotlin.srcDir("src/sembrador/java")
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
    androidTestImplementation(libs.androidx.room.testing)
    // room-testing (MigrationTestHelper) pide serialization 1.8.1 y la app trae
    // 1.7.3 via lifecycle. En un test instrumentado manda la version del APK de la
    // app: sin esto, AbstractMethodError al leer 1.json. Solo debug; release no cambia.
    debugImplementation(libs.kotlinx.serialization.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
