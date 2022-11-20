plugins {
    kotlin("jvm")
}

group = "altline.appliance"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(platform(project(":platform")))

    api(Libs.KotlinxCoroutines.core)
}