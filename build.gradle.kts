plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"

    id("net.mamoe.mirai-console") version "2.12.0"
    id("net.mamoe.maven-central-publish") version "0.7.1"
}

group = "xyz.cssxsh.mirai"
version = "2.2.6"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("cssxsh", "mirai-hibernate-plugin")
    licenseFromGitHubProject("AGPL-3.0", "master")
    publication {
        artifact(tasks.getByName("buildPlugin"))
        artifact(tasks.getByName("buildPluginLegacy"))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-slf4j-bridge:1.2.0")
    compileOnly("net.mamoe:mirai-core:2.12.0")
    compileOnly("net.mamoe:mirai-core-utils:2.12.0")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.2.0")
    // SQL/ORM
    api("org.hibernate:hibernate-core:5.6.9.Final")
    api("org.hibernate:hibernate-hikaricp:5.6.9.Final") {
        exclude("org.slf4j")
    }
    api("com.zaxxer:HikariCP:5.0.1") {
        exclude("org.slf4j")
    }
    api("com.github.gwenn:sqlite-dialect:0.1.2")
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.36.0.3")
    api("mysql:mysql-connector-java:8.0.29")
    api("org.postgresql:postgresql:42.3.6")

    testImplementation(kotlin("test", "1.6.21"))
}

kotlin {
    explicitApi()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
