plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.jpa") version "1.7.21"
    kotlin("plugin.serialization") version "1.7.21"

    id("net.mamoe.mirai-console") version "2.13.2"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "2.5.1"

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
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.3.0")
    testImplementation(kotlin("test"))
    //
    implementation(platform("net.mamoe:mirai-bom:2.13.2"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-core-utils")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.5"))
    testImplementation("org.slf4j:slf4j-simple")
    // SQL/ORM
    api("org.hibernate.orm:hibernate-core:6.1.6.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.1.6.Final")
    api("org.hibernate.orm:hibernate-community-dialects:6.1.6.Final")
    api("com.zaxxer:HikariCP:5.0.1")
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.40.0.0")
    api("mysql:mysql-connector-java:8.0.31")
    api("org.postgresql:postgresql:42.5.1")
    api("org.reflections:reflections:0.10.2")
    api("com.google.protobuf:protobuf-java:3.21.9")
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
