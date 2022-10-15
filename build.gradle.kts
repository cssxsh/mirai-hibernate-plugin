plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    kotlin("plugin.jpa") version "1.7.20"

    id("net.mamoe.mirai-console") version "2.13.0-RC"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "2.4.4"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("cssxsh", "mirai-hibernate-plugin")
    licenseFromGitHubProject("AGPL-3.0")
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: project.buildDir.resolve("publishing-tmp")
    publication {
        artifact(tasks.getByName("buildPlugin"))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-core:2.13.0-RC")
    compileOnly("net.mamoe:mirai-core-utils:2.13.0-RC")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.2.8")
    // SQL/ORM
    api("org.hibernate.orm:hibernate-core:6.1.4.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.1.4.Final") {
        exclude("org.slf4j")
    }
    api("org.hibernate.orm:hibernate-community-dialects:6.1.4.Final")
    api("com.zaxxer:HikariCP:5.0.1") {
        exclude("org.slf4j")
    }
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.39.3.0")
    api("mysql:mysql-connector-java:8.0.30")
    api("org.postgresql:postgresql:42.5.0")
    implementation("org.reflections:reflections:0.10.2") {
        exclude("org.slf4j")
    }

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.1")
    testImplementation("net.mamoe:mirai-logging-slf4j:2.13.0-RC")
    testImplementation("net.mamoe:mirai-core-utils:2.13.0-RC")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}

kotlin {
    explicitApi()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
