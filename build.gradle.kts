plugins {
    java
}

group = "dev.defaultchat"
version = "1.0.0"

// Compile against the oldest supported Paper API by default so a single jar keeps
// working on every newer release. Override with -PpaperApi=<version> to verify
// against the newest API line (for example -PpaperApi=26.3.build.248-stable).
val paperApi: String = providers.gradleProperty("paperApi").getOrElse("1.21.11-R0.1-SNAPSHOT")

// 26.2+ ships Adventure 5, older releases ship Adventure 4. Keep the test runtime
// aligned with whatever the compile classpath resolves so both builds test cleanly.
val adventureVersion: String = if (paperApi.startsWith("26.2") || paperApi.startsWith("26.3")) {
    "5.2.0"
} else {
    "4.26.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("net.kyori:adventure-text-minimessage:$adventureVersion")
    testImplementation("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("DefaultChat")
    manifest {
        attributes("Implementation-Version" to project.version)
    }
}
