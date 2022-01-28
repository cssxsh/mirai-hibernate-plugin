plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    kotlin("plugin.jpa") version "1.6.0"

    id("net.mamoe.mirai-console") version "2.10.0-RC2"
    id("net.mamoe.maven-central-publish") version "0.7.0"
}

group = "xyz.cssxsh.mirai"
version = "1.0.4"


mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("cssxsh", "mirai-hibernate-plugin")
    licenseFromGitHubProject("AGPL-3.0", "master")
    publication {
        artifact(tasks.getByName("buildPlugin"))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // SQL/ORM
    api("org.hibernate:hibernate-core:5.6.4.Final")
    api("org.hibernate:hibernate-c3p0:5.6.4.Final")
    api("com.github.gwenn:sqlite-dialect:0.1.2")
    api("org.xerial:sqlite-jdbc:3.36.0.3")
    api("mysql:mysql-connector-java:8.0.26")

    testImplementation(kotlin("test", "1.6.0"))
}

kotlin {
    explicitApi()
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
    configureShadow {
        exclude("module-info.class")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
