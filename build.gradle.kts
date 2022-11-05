plugins {
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    kotlin("plugin.jpa") version "1.7.20"

    id("net.mamoe.mirai-console") version "2.13.0-RC2"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "2.5.0-RC2"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("cssxsh", "mirai-hibernate-plugin")
    licenseFromGitHubProject("AGPL-3.0")
    workingDir = System.getenv("PUBLICATION_TEMP")?.let { file(it).resolve(projectName) }
        ?: buildDir.resolve("publishing-tmp")
    publication {
        artifact(tasks["buildPlugin"])
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("net.mamoe:mirai-core:2.13.0-RC2")
    compileOnly("net.mamoe:mirai-core-utils:2.13.0-RC2")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.2.9")
    // SQL/ORM
    api("org.hibernate.orm:hibernate-core:6.1.5.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.1.5.Final") {
        exclude(group = "org.slf4j")
    }
    api("org.hibernate.orm:hibernate-community-dialects:6.1.5.Final")
    api("com.zaxxer:HikariCP:5.0.1") {
        exclude(group = "org.slf4j")
    }
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.39.3.0")
    api("mysql:mysql-connector-java:8.0.31")
    api("org.postgresql:postgresql:42.5.0")
    api("org.reflections:reflections:0.10.2") {
        exclude(group = "org.slf4j")
    }
    implementation("com.google.protobuf:protobuf-java:3.21.9")

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.3")
    testImplementation("net.mamoe:mirai-logging-slf4j:2.13.0-RC2")
    testImplementation("net.mamoe:mirai-core-utils:2.13.0-RC2")
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
