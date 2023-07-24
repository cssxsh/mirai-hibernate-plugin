plugins {
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.jpa") version "1.8.10"
    kotlin("plugin.serialization") version "1.8.10"

    id("net.mamoe.mirai-console") version "2.15.0"
    id("me.him188.maven-central-publish") version "1.0.0-dev-3"
}

group = "xyz.cssxsh.mirai"
version = "2.7.1"

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
    api("org.hibernate.orm:hibernate-core:6.2.6.Final")
    api("org.hibernate.orm:hibernate-hikaricp:6.2.6.Final")
    api("org.hibernate.orm:hibernate-community-dialects:6.2.6.Final")
    api("com.zaxxer:HikariCP:5.0.1")
    api("com.h2database:h2:2.1.214")
    api("org.xerial:sqlite-jdbc:3.42.0.0")
    api("com.mysql:mysql-connector-j:8.0.33")
    api("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    api("org.postgresql:postgresql:42.6.0")
    api("org.reflections:reflections:0.10.2")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.4.0")
    testImplementation(kotlin("test"))
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.2.0.jre11")
    //
    implementation(platform("net.mamoe:mirai-bom:2.15.0"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-console-compiler-common")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.7"))
    testImplementation("org.slf4j:slf4j-simple")
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
