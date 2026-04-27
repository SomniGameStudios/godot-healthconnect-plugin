import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.android)
}

val pluginName = "HealthConnectPlugin"
val pluginPackageName = "com.somnigamestudios.healthconnect"

android {
    namespace = pluginPackageName
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 26
        manifestPlaceholders["godotPluginName"] = pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginName}\"")
        setProperty("archivesBaseName", pluginName)
    }

    /*buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }*/

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    //implementation(libs.androidx.core.ktx)
    //implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    //androidTestImplementation(libs.androidx.espresso.core)

    // Godot dependencies
    implementation("org.godotengine:godot:4.6.2.stable")

    // Common dependencies
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Health Connect dependencies
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // Sensor dependencies
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material")

    // WorkManager Dependencies
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}

// BUILD TASKS DEFINITION
val copyDebugAARToGodotAddons by tasks.registering(Copy::class) {
    description = "Copies the generated debug AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-debug.aar")
    into("../../godot_editor/addons/healthconnect_plugin/bin")
}

val copyReleaseAARToGodotAddons by tasks.registering(Copy::class) {
    description = "Copies the generated release AAR binary to the plugin's addons directory"
    from("build/outputs/aar")
    include("$pluginName-release.aar")
    into("../../godot_editor/addons/healthconnect_plugin/bin")
}

val cleanGodotAddons by tasks.registering(Delete::class) {
    // delete("../../godot_editor/addons/healthconnect_plugin") // Don't delete the whole plugin folder
}

val copyAddonsToGodot by tasks.registering(Copy::class) {
    description = "Copies the export scripts templates to the plugin's addons directory"

    dependsOn(cleanGodotAddons)
    finalizedBy(copyDebugAARToGodotAddons)
    finalizedBy(copyReleaseAARToGodotAddons)

    from("export_scripts_template")
    into("../../godot_editor/addons/healthconnect_plugin")
}

tasks.named("assemble").configure {
    finalizedBy(copyAddonsToGodot)
}

tasks.named<Delete>("clean").apply {
    dependsOn(cleanGodotAddons)
}