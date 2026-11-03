package org.openyaske.tool.syllabus

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.validation.SyllabusBundle
import org.openyaske.core.validation.SyllabusBundleCodec
import org.openyaske.core.validation.SyllabusManifest
import org.openyaske.core.validation.SyllabusTsvParser
import java.io.File
import java.security.MessageDigest
import java.time.Instant

private val FILE = Regex("""syllabus_(\d{4})\.tsv""")

fun main(args: Array<String>) {
    val inputDir = File(args.getOrElse(0) { "data/syllabus" })
    val outputDir = File(
        args.getOrElse(1) { "composeApp/src/commonMain/composeResources/files" }
    )
    require(inputDir.isDirectory) { "input directory not found: ${inputDir.absolutePath}" }

    val files = inputDir.listFiles { file -> FILE.matches(file.name) }
        .orEmpty()
        .sortedBy { it.name }

    require(files.isNotEmpty()) { "no syllabus_YYYY.tsv in ${inputDir.absolutePath}" }

    val entries = ArrayList<SyllabusEntry>()
    val years = ArrayList<Int>()
    for (file in files) {
        val year = FILE.matchEntire(file.name)?.groupValues?.get(1)?.toInt()
            ?: error("unexpected file name: ${file.name}")
        val parsed = SyllabusTsvParser.parse(file.readText(Charsets.UTF_8), year)
        years += year
        entries += parsed
        println("${file.name}: ${parsed.size} entries")
    }

    val unique = entries.associateBy { it.id }.values
        .sortedWith(compareBy({ it.year }, { it.code }, { it.title }))
    val generatedAt = Instant.now().toString()
    val bytes = SyllabusBundleCodec.encode(unique, generatedAt)

    outputDir.mkdirs()
    val bundleFile = File(outputDir, "syllabus.cbor")
    bundleFile.writeBytes(bytes)

    val digest = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
    val manifest = SyllabusManifest(
        schemaVersion = SyllabusBundle.CURRENT_SCHEMA_VERSION,
        generatedAt = generatedAt,
        entryCount = unique.size,
        bundleBytes = bytes.size.toLong(),
        bundleSha256 = digest,
        years = years
    )
    val json = Json { prettyPrint = true }
    File(outputDir, "syllabus-manifest.json")
        .writeText(json.encodeToString(SyllabusManifest.serializer(), manifest) + "\n")

    val byTerm = unique.groupingBy { "${it.year} ${it.semester}" }.eachCount()
        .toList()
        .sortedBy { it.first }
    for ((term, count) in byTerm) {
        println("  $term: $count")
    }
    println("wrote ${bundleFile.path}: ${bytes.size} bytes, ${unique.size} entries")
    println("years: ${years.joinToString()}")
    println("sha256: $digest")
}
