import net.fabricmc.loom.LoomGradleExtension

plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
    id("net.fabricmc.fabric-loom-remap") version "1.17.+"
    id("ploceus") version "1.17.+"
}

group = "dev.rdh"
version = "1.0.0"

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

fun v(n: String) = providers.gradleProperty("${n}_version").get()

repositories {
    exclusiveContent {
        forRepository { mavenCentral() }
        filter { includeGroup("org.lwjgl") }
    }
    maven("https://maven.taumc.org/releases")
    maven("https://maven.cloverclient.com/releases")
    maven("https://repo.lucko.me/")
    exclusiveContent {
        forRepository { maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1") }
        filter { includeGroup("me.djtheredstoner") }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

val shade = configurations.create("shade")
configurations.implementation.get().extendsFrom(shade)

dependencies {
    minecraft("com.mojang:minecraft:${v("minecraft")}")
    mappings(loom.layered {
        mappings(ploceus.featherMappings(v("feather")))
    })

    modImplementation("net.fabricmc:fabric-loader:${v("fabric")}")
    ploceus.dependOsl(v("osl"))

    modImplementation("pl.tomgirl:pylon:${v("pylon")}")
    shade("me.lucko:spark-common:${v("spark")}") {
        exclude(group = "org.ow2.asm")
    }
    compileOnly(files(shade))

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${v("devauth")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
}

configurations.all {
    resolutionStrategy {
        exclude(group = "org.lwjgl.lwjgl")
    }
}

loom.runs.named("client") {
    jvmArguments.addAll(
        "-XstartOnFirstThread",
        "-XX:+UseZGC", "-XX:MaxGCPauseMillis=50", "-XX:+UseCompactObjectHeaders",
        "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow",
    )
    systemProperties.putAll(providers.gradlePropertiesPrefixedBy("run.").map {
        it.mapKeys { e -> e.key.removePrefix("run.") }
    })
}

gradle.taskGraph.whenReady {
    allTasks.filter { it.name == "net.fabricmc.devlaunchinjector.Main.main()" }.forEach {
        it.notCompatibleWithConfigurationCache("loom weird?")
    }
}

abstract class GenerateMappings : DefaultTask() {
    @get:InputFile
    abstract val tiny: RegularFileProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generate() {
        val out = linkedMapOf<String, String>()
        tiny.get().asFile.bufferedReader().use { reader ->
            val namespaces = reader.readLine().split('\t').drop(3)
            val named = namespaces.indexOf("named")
            val intermediary = namespaces.indexOf("intermediary")
            require(named >= 0 && intermediary >= 0) { "need named + intermediary, got $namespaces" }

            reader.forEachLine { line ->
                val depth = line.length - line.trimStart('\t').length
                val parts = line.trim('\t', '\n').split('\t')
                val names = when {
                    depth == 0 && parts[0] == "c" -> parts.drop(1)
                    depth == 1 && parts[0] == "m" -> parts.drop(2)
                    else -> return@forEachLine
                }
                if (names.size <= maxOf(named, intermediary)) return@forEachLine
                if (names[intermediary] != names[named]) out[names[intermediary]] = names[named]
            }
        }

        val file = output.get().asFile.also { it.parentFile.mkdirs() }
        file.bufferedWriter().use { writer ->
            out.forEach { (intermediary, named) -> writer.append(intermediary).append('\t').append(named).append('\n') }
        }
        logger.lifecycle("pulse: ${out.size} mappings, ${file.length()} B")
    }
}

val generateMappings = tasks.register<GenerateMappings>("generateMappings") {
    output = layout.buildDirectory.file("generated/pulse/mappings.txt")
}

afterEvaluate {
    val tinyFile = LoomGradleExtension.get(project).mappingConfiguration.tinyMappings.toFile()
    generateMappings.configure { tiny = tinyFile }
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }

    from("LICENSE")

    from(generateMappings) {
        into("assets/pulse")
    }
}

tasks.assemble {
    dependsOn("remapJar")
}

tasks.shadowJar {
    configurations = listOf(shade)
    archiveClassifier = "dev-shadow"
    relocate("net.kyori", "me.lucko.spark.lib.adventure")
    relocate("net.bytebuddy", "me.lucko.spark.lib.bytebuddy")
    relocate("com.google.protobuf", "me.lucko.spark.lib.protobuf")
    relocate("me.lucko.bytesocks", "me.lucko.spark.lib.bytesocks")
    relocate("org.java_websocket", "me.lucko.spark.lib.bytesocks.ws")
}

tasks.remapJar {
    dependsOn(tasks.shadowJar)
    inputFile = tasks.shadowJar.flatMap { it.archiveFile }
}
