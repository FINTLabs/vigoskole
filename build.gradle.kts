plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("com.diffplug.spotless") version "8.5.0"
}

val spotlessRatchetRef =
    providers
        .gradleProperty("spotlessRatchetFrom")
        .orElse("origin/main")

group = "no.novari"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.fintlabs.no/releases")
}

val eclipseStoreJvmArgs = listOf("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED")

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.5"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.5"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.eclipse.store:storage-embedded:4.0.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.11")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs(eclipseStoreJvmArgs)
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs(eclipseStoreJvmArgs)
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootTestRun") {
    jvmArgs(eclipseStoreJvmArgs)
}

spotless {
    java {
        spotlessRatchetRef.orNull
            ?.takeIf(String::isNotBlank)
            ?.let(::ratchetFrom)
        googleJavaFormat("1.31.0")
        target("src/*/java/**/*.java")
        importOrder()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register("format") {
    group = "formatting"
    description = "Formats changed Java source files with Spotless."
    dependsOn("spotlessApply")
}
