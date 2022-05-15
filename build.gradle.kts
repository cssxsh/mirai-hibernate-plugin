plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"
    kotlin("plugin.jpa") version "1.6.0"

    id("net.mamoe.mirai-console") version "2.10.3"
    id("net.mamoe.maven-central-publish") version "0.7.1"
}

group = "xyz.cssxsh.mirai"
version = "2.2.0"

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
}

dependencies {
    compileOnly("net.mamoe:mirai-slf4j-bridge:1.2.0")
    compileOnly("net.mamoe:mirai-core:2.10.3")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.0.7")
    // SQL/ORM
    api("org.hibernate:hibernate-core:5.6.8.Final")
    api("org.hibernate:hibernate-hikaricp:5.6.8.Final") {
        exclude("org.slf4j")
    }
    api("com.zaxxer:HikariCP:4.0.3") {
        exclude("org.slf4j")
    }
    api("com.github.gwenn:sqlite-dialect:0.1.2")
    api("org.xerial:sqlite-jdbc:3.36.0.3")
    api("mysql:mysql-connector-java:8.0.29")

    testImplementation(kotlin("test", "1.6.0"))
}

kotlin {
    explicitApi()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
