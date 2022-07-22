plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"

    id("net.mamoe.mirai-console") version "2.12.0"
    id("net.mamoe.maven-central-publish") version "0.7.1"
}

group = "xyz.cssxsh.mirai"
version = "2.4.1"

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
    compileOnly("net.mamoe:mirai-core:2.12.0")
    compileOnly("net.mamoe:mirai-core-utils:2.12.0")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.2.0")
    // SQL/ORM
    api("org.hibernate.orm:hibernate-core:6.1.1.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.1.1.Final") {
        exclude("org.slf4j")
    }
    api("org.hibernate.orm:hibernate-community-dialects:6.1.1.Final")
    api("com.zaxxer:HikariCP:5.0.1") {
        exclude("org.slf4j")
    }
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.36.0.3")
    api("mysql:mysql-connector-java:8.0.29")
    api("org.postgresql:postgresql:42.4.0")

    testImplementation(kotlin("test", "1.6.21"))
    testImplementation("net.mamoe:mirai-core-utils:2.12.0")
    testImplementation("net.mamoe:mirai-slf4j-bridge:1.2.0")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
}

noArg {
    annotation("jakarta.persistence.Entity")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
