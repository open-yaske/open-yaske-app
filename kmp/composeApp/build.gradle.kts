@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
	kotlin("multiplatform")
	id("com.android.application")
	id("org.jetbrains.compose")
	id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
	androidTarget {
		compilerOptions {
			jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
		}
	}

	wasmJs {
		browser {
			commonWebpackConfig {
				outputFileName = "composeApp.js"
			}
		}
		binaries.executable()
	}

	listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
		target.binaries.framework {
			baseName = "ComposeApp"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(project(":shared"))
			implementation(compose.runtime)
			implementation(compose.foundation)
			implementation(compose.material3)
			// Expressive theme は alpha 系にしか公開されていない
			implementation("org.jetbrains.compose.material3:material3:1.11.0-alpha07")
			implementation(compose.materialIconsExtended)
			implementation(compose.ui)
			implementation(compose.components.resources)
			implementation("org.jetbrains.compose.ui:ui-backhandler:1.11.1")
		}
		androidMain.dependencies {
			implementation("androidx.activity:activity-compose:1.12.4")
			implementation("androidx.core:core-splashscreen:1.0.1")
			implementation("androidx.work:work-runtime-ktx:2.9.1")
		}
		wasmJsMain.dependencies {
			implementation("org.jetbrains.kotlinx:kotlinx-browser:0.3")
		}
	}
}

compose.resources {
	publicResClass = true
	packageOfResClass = "org.openyaske.app.resources"
}

android {
	namespace = "org.openyaske.app"
	compileSdk = 36

	defaultConfig {
		applicationId = "org.openyaske.app"
		minSdk = 26
		targetSdk = 36
		versionCode = 1
		versionName = "0.1.0"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	buildFeatures {
		compose = true
	}
}
