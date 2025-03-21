import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.8.21"
    id("java")
    kotlin("jvm") version kotlinVersion
    id("maven-publish")
    id("java-library")
    id("org.jetbrains.dokka") version "1.7.20"
}

java.sourceCompatibility = JavaVersion.VERSION_1_9
java.targetCompatibility = JavaVersion.VERSION_1_9

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/atelier-saulx/based-android")
            credentials {
                username = "SteliosPapamichail"
                password = System.getenv("BASED_ANDROID_KEY")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("org.slf4j:slf4j-api:1.7.30")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")

    implementation("com.squareup.okhttp3:okhttp:4.9.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "9"
    }
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
