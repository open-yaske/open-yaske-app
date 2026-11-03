plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	application
}

kotlin {
	jvmToolchain(21)
}

dependencies {
	implementation(project(":shared"))
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

application {
	mainClass.set("org.openyaske.tool.syllabus.MainKt")
}

tasks.named<JavaExec>("run") {
	workingDir = rootProject.projectDir
}
