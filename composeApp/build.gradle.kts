import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    //el plugin.serialization: Es una herramienta que convierte las clases
    //de kotlin en texto JSON y viceversa de forma rápida
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }



    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)

            //Motor para android
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.content.negotiation)
            val androidMain by getting {
                dependencies {
                    implementation(libs.ktor.client.okhttp)

                }
            }


        }
        commonMain.dependencies {
            //dependencias de ktor para la API
            //ktor-client-core (Este es el motor base para enviar y recibir datos)
            implementation(libs.ktor.client.core)
            //.content-negotiation: Es el "traductor". Permite que ktor entienda que lo que viaja por internet es JSON
            implementation(libs.ktor.client.content.negotiation)
            //Es el formato específico que usara el traductor
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("androidx.datastore:datastore-preferences:1.2.1")

            //Motor para iOS
            iosMain.dependencies { implementation(libs.ktor.client.darwin) }

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("androidx.navigation:navigation-compose:2.7.7")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.nexo.app.nexo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.nexo.app.nexo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.12")
    debugImplementation(libs.compose.uiTooling)
}

