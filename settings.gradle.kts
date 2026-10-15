rootProject.name = "nexo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            /*mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }*/
        }
        mavenCentral{
            /*mavenContent {
                includeGroupAndSubgroups("org.jetbrains")
            }*/
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            /*mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }*/
        }
        mavenCentral{
           /* mavenContent {
                includeGroupAndSubgroups("org.jetbrains")
                includeGroupAndSubgroups("io.ktor")
                includeGroupAndSubgroups("com.squareup") //para okHttp y Okio
                includeGroupAndSubgroups("junit") //para las pruebas
                includeGroupAndSubgroups("org.slf4j") //para los logs
                includeGroupAndSubgroups("com.google.guava") //para utilidades de google
                includeGroupAndSubgroups("org.jspecify")
            }*/
        }
    }
}

include(":composeApp")
//include(":nexo-server")