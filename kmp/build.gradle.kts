plugins {
	kotlin("multiplatform") version "2.4.21" apply false
	kotlin("jvm") version "2.4.21" apply false
	kotlin("plugin.serialization") version "2.4.21" apply false
	id("com.android.library") version "8.13.2" apply false
	id("com.android.application") version "8.13.2" apply false
	id("org.jetbrains.compose") version "1.11.1" apply false
	id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
	id("app.cash.sqldelight") version "2.4.1" apply false
}
