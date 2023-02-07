plugins {
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"
    kotlin("plugin.serialization") version "1.7.22"

    id("net.mamoe.mirai-console") version "2.14.0-RC"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "2.6.1"

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
    api("org.hibernate.orm:hibernate-core:6.1.6.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.1.6.Final")
    api("org.hibernate.orm:hibernate-community-dialects:6.1.6.Final")
    api("com.zaxxer:HikariCP:5.0.1")
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.40.1.0")
    api("com.mysql:mysql-connector-j:8.0.32")
    api("org.postgresql:postgresql:42.5.3")
    api("org.reflections:reflections:0.10.2")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.3.0")
    testImplementation(kotlin("test"))
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")
    //
    implementation(platform("net.mamoe:mirai-bom:2.14.0-RC"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-core-utils")
    testCompileOnly("net.mamoe:mirai-console-compiler-common")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.6"))
    testImplementation("org.slf4j:slf4j-simple")
    //
    implementation(platform("com.google.protobuf:protobuf-bom:3.21.12"))
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
