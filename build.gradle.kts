plugins {
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.jpa") version "1.8.22"
    kotlin("plugin.serialization") version "1.8.22"

    id("net.mamoe.mirai-console") version "2.16.0"
    id("me.him188.maven-central-publish") version "1.0.0"
}

group = "xyz.cssxsh.mirai"
version = "2.8.0"

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
    api("com.zaxxer:HikariCP:5.1.0")
    api("com.h2database:h2:2.2.224")
    api("org.xerial:sqlite-jdbc:3.45.3.0")
    api("com.mysql:mysql-connector-j:8.3.0")
    api("org.mariadb.jdbc:mariadb-java-client:3.3.3")
    api("org.postgresql:postgresql:42.7.3")
    api("org.reflections:reflections:0.10.2")
    compileOnly("xyz.cssxsh.mirai:mirai-administrator:1.4.0")
    testImplementation(kotlin("test"))
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:12.6.1.jre11")
    //
    implementation(platform("net.mamoe:mirai-bom:2.16.0"))
    compileOnly("net.mamoe:mirai-core")
    compileOnly("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-logging-slf4j")
    testImplementation("net.mamoe:mirai-core-mock")
    testImplementation("net.mamoe:mirai-core-utils")
    testImplementation("net.mamoe:mirai-console-compiler-common")
    //
    implementation(platform("org.hibernate.orm:hibernate-platform:6.4.4.Final"))
    api("org.hibernate.orm:hibernate-core")
    api("org.hibernate.orm:hibernate-hikaricp")
    api("org.hibernate.orm:hibernate-community-dialects")
    //
    implementation(platform("org.slf4j:slf4j-parent:2.0.13"))
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
