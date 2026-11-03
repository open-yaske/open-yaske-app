@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
	id("com.android.library")
	id("app.cash.sqldelight")
}

kotlin {
	androidTarget {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}

	jvm {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}

	// Web (wasm) — Svelte 版の置き換え先
	wasmJs {
		browser()
		nodejs()
	}

	// iOS は macOS 上でのみコンパイル可能。フレームワークを出力する。
	val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())
	iosTargets.forEach { target ->
		target.binaries.framework {
			baseName = "OpenYaskeCore"
			isStatic = true
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
			implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0")
			implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
			implementation("app.cash.sqldelight:runtime:2.4.1")
		}
		commonTest.dependencies {
			implementation(kotlin("test"))
		}
		jvmMain.dependencies {
			implementation("app.cash.sqldelight:sqlite-driver:2.4.1")
		}
		androidMain.dependencies {
			implementation("app.cash.sqldelight:android-driver:2.4.1")
		}
		iosMain.dependencies {
			implementation("app.cash.sqldelight:native-driver:2.4.1")
		}
	}
}

sqldelight {
	databases {
		create("OpenYaskeDatabase") {
			packageName.set("org.openyaske.core.db")
		}
	}
}

android {
	namespace = "org.openyaske.core"
	compileSdk = 36

	defaultConfig {
		minSdk = 26
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}
}
