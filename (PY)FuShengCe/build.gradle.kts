import com.github.jk1.license.filter.SpdxLicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.JsonReportRenderer
import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    id("com.github.jk1.dependency-license-report") version "3.1.4"
}

licenseReport {
    projects = project.allprojects.toTypedArray()
    configurations = arrayOf("debugRuntimeClasspath")
    filters = arrayOf(SpdxLicenseBundleNormalizer())
    renderers = arrayOf(
        InventoryHtmlReportRenderer("index.html", "FuShengCe dependency licenses"),
        JsonReportRenderer("licenses.json"),
    )
    allowedLicensesFile = layout.projectDirectory.file("compliance/allowlisted-licenses.json")
}

tasks.register("verifyNoRestrictedAssets") {
    group = "verification"
    description = "Rejects bundled AI models and copied brands in production source sets."

    val inspectedRoots = subprojects.map { it.layout.projectDirectory.dir("src/main") }

    inputs.files(inspectedRoots)

    doLast {
        val forbiddenExtensions = setOf("onnx", "tflite", "pt", "pth", "safetensors")
        val forbiddenBrands = listOf("ReFra", "Immich", "Fossify")
        val violations = mutableListOf<String>()

        inspectedRoots.forEach { root ->
            if (!root.asFile.exists()) return@forEach
            root.asFile.walkTopDown().filter { it.isFile }.forEach { file ->
                if (file.extension.lowercase() in forbiddenExtensions) {
                    violations += "model asset: ${file.relativeTo(projectDir)}"
                }
                if (file.extension.lowercase() in setOf("kt", "kts", "xml", "txt", "json", "properties")) {
                    val text = file.readText()
                    forbiddenBrands.filter(text::contains).forEach { brand ->
                        violations += "copied brand '$brand': ${file.relativeTo(projectDir)}"
                    }
                }
            }
        }

        check(violations.isEmpty()) {
            "Restricted assets or copied brands found:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.register("verifyProductionAssets") {
    group = "verification"
    description = "Validates the two frozen visual masters plus retained font, animation and sound assets."

    val largeMaster = layout.projectDirectory.file(
        "feature/home/src/main/res/drawable-nodpi/fushengce_home_large_master.png",
    )
    val smallMaster = layout.projectDirectory.file(
        "feature/home/src/main/res/drawable-nodpi/fushengce_home_small_master.png",
    )
    val animation = layout.projectDirectory.file(
        "feature/home/src/main/res/raw/character_feedback.json",
    )
    val interfaceFont = layout.projectDirectory.file(
        "app/src/main/res/font/fusheng_wenkai.ttf",
    )
    val interfaceFontLicense = layout.projectDirectory.file(
        "compliance/licenses/OFL-LXGW-WenKai-Lite.txt",
    )
    val soundDirectory = layout.projectDirectory.dir("app/src/main/res/raw")
    val inventory = layout.projectDirectory.file("compliance/ASSET_LICENSES.md")
    val expectedSounds = setOf(
        "affairs_ambient.wav", "album_open.wav", "armor_shift.wav", "brush_write.wav",
        "complete.wav", "found.wav", "issue.wav", "life_ambient.wav",
        "listen_start.wav", "page_flip.wav", "realm_switch.wav", "seal_stamp.wav",
        "task_end.wav", "unavailable.wav",
    )
    val expectedMarkers = setOf(
        "idle", "listening", "thinking", "found", "remind", "complete",
        "not_found", "error", "realm_switch",
    )

    inputs.files(
        largeMaster, smallMaster, animation, interfaceFont,
        interfaceFontLicense, soundDirectory, inventory,
    )

    doLast {
        fun verifyFrozenPng(
            file: java.io.File,
            expectedBytes: Int,
            expectedSha256: String,
        ) {
            check(file.isFile) { "Missing frozen visual master: ${file.relativeTo(projectDir)}" }
            val bytes = file.readBytes()
            check(bytes.size == expectedBytes) {
                "Frozen visual master byte size changed: ${file.relativeTo(projectDir)}"
            }
            check(bytes.size > 8 && bytes.copyOfRange(1, 4).decodeToString() == "PNG") {
                "Frozen visual master is not a PNG: ${file.relativeTo(projectDir)}"
            }
            val actualSha256 = java.security.MessageDigest.getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(actualSha256 == expectedSha256) {
                "Frozen visual master changed: ${file.relativeTo(projectDir)}"
            }
        }

        verifyFrozenPng(
            largeMaster.asFile,
            2_639_041,
            "3461669f81dbbc4c581b6c42a57051bcad11dc962b1a880bffc8f5ab2d3724c5",
        )
        verifyFrozenPng(
            smallMaster.asFile,
            2_755_642,
            "5d616b3bde5c8191480bb510e3981e1ccce306da2f46d23cd8db912fa576cbff",
        )

        check(interfaceFont.asFile.isFile && interfaceFont.asFile.length() > 100_000L) {
            "Missing or incomplete FuSheng WenKai interface font"
        }
        val fontHeader = interfaceFont.asFile.readBytes().take(4).toByteArray()
        check(fontHeader.contentEquals(byteArrayOf(0, 1, 0, 0))) {
            "FuSheng WenKai interface font is not a TrueType font"
        }
        check(interfaceFontLicense.asFile.readText().contains("SIL OPEN FONT LICENSE Version 1.1")) {
            "FuSheng WenKai interface font license is missing or invalid"
        }

        val actualSounds = soundDirectory.asFile.listFiles()
            ?.filter { it.isFile && it.extension == "wav" }
            ?.mapTo(mutableSetOf()) { it.name }
            .orEmpty()
        check(actualSounds == expectedSounds) {
            "Unexpected sound set. Expected $expectedSounds, found $actualSounds"
        }

        val animationText = animation.asFile.readText()
        expectedMarkers.forEach { marker ->
            check("\"cm\": \"$marker\"" in animationText) {
                "Missing Lottie marker: $marker"
            }
        }

        val inventoryText = inventory.asFile.readText()
        listOf(
            "fushengce_home_large_master.png",
            "fushengce_home_small_master.png",
            "character_feedback.json",
            "fusheng_wenkai.ttf",
            "app/src/main/res/raw/*.wav",
        ).forEach { asset ->
            check(asset in inventoryText) { "Asset is not documented: $asset" }
        }
    }
}

tasks.named("verifyNoRestrictedAssets") {
    dependsOn("verifyProductionAssets")
}

tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Checks project dependency boundaries and rejects module cycles."

    doLast {
        val allowedFeatureDependencies = mapOf(
            ":feature:gallery" to setOf(":feature:search", ":feature:albums", ":feature:viewer"),
            ":feature:tasks" to setOf(":feature:search", ":feature:viewer"),
        )
        val graph = subprojects.associate { module ->
            module.path to module.configurations.flatMap { configuration ->
                configuration.dependencies.withType(ProjectDependency::class.java)
                    // Android test classpaths automatically depend on their own production module.
                    .filterNot { configuration.isCanBeResolved && it.path == module.path }
                    .map { it.path }
            }.toSet()
        }
        val violations = mutableListOf<String>()
        graph.keys.filterNot {
            it in setOf(":app", ":core", ":feature") ||
                it.startsWith(":core:") || it.startsWith(":feature:")
        }.forEach { violations += "Unclassified module: $it (use :core:* or :feature:*)" }
        graph.forEach { (source, targets) ->
            targets.forEach { target ->
                if (source in setOf(":core", ":feature") || target in setOf(":core", ":feature") ||
                    source != ":app" && target == ":app" ||
                    source.startsWith(":core:") && target.startsWith(":feature:") ||
                    source.startsWith(":feature:") && target.startsWith(":feature:") &&
                    target !in allowedFeatureDependencies[source].orEmpty()
                ) {
                    violations += "Forbidden dependency: $source -> $target"
                }
            }
        }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(module: String) {
            if (module in visited) return
            check(visiting.add(module)) { "Module dependency cycle: ${visiting.joinToString(" -> ")} -> $module" }
            graph[module].orEmpty().forEach(::visit)
            visiting.remove(module)
            visited.add(module)
        }
        graph.keys.forEach(::visit)
        check(violations.isEmpty()) { violations.joinToString("\n") }
    }
}

tasks.register("verifyFoundation") {
    group = "verification"
    description = "Builds, lints and verifies the foundation; checkLicense remains an independent release gate."
    dependsOn("verifyNoRestrictedAssets", "verifyModuleBoundaries")
    dependsOn(provider {
        subprojects.flatMap { module ->
            module.tasks.matching { it.name in setOf("assembleDebug", "lintDebug") }.toList()
        }
    })
}
