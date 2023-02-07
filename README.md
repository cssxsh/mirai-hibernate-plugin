# [Mirai Hibernate Plugin](https://github.com/cssxsh/mirai-hibernate-plugin)

> Mirai Hibernate 前置插件

[Mirai Console](https://github.com/mamoe/mirai-console) 的前置插件，用于 Hibernate ORM 框架的初始化

[![maven-central](https://img.shields.io/maven-central/v/xyz.cssxsh.mirai/mirai-hibernate-plugin)](https://search.maven.org/artifact/xyz.cssxsh.mirai/mirai-hibernate-plugin)
[![Database Test](https://github.com/cssxsh/mirai-hibernate-plugin/actions/workflows/test.yml/badge.svg)](https://github.com/cssxsh/mirai-hibernate-plugin/actions/workflows/test.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/f82572fd42324ce19df9d1639250127d)](https://www.codacy.com/gh/cssxsh/mirai-hibernate-plugin/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=cssxsh/mirai-hibernate-plugin&amp;utm_campaign=Badge_Grade)

插件自带聊天记录器 [MiraiHibernateRecorder](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateLoader.kt),  
会记录 `群聊/私聊` 的内容到数据库方便其他插件使用，默认是 `h2database` 数据库(since `2.2.0+`)  
数据库配置在 `config/xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin/hibernate.properties`  
`2.2.0` 之前的版本的 默认数据库 为 `sqlite`, 你可以直接删掉 `hibernate.properties`, 让其重新生成 `h2database` 配置  

## 数据库支持

本插件打包了以下版本的数据库驱动和连接池

*   `com.mysql:mysql-connector-j:8.0.32` - [mysql.hibernate.properties](example/mysql.hibernate.properties)
*   `org.xerial:sqlite-jdbc:3.40.0.0` - [sqlite.hibernate.properties](example/sqlite.hibernate.properties)
*   `org.postgresql:postgresql:42.5.1` - [postgresql.hibernate.properties](example/postgresql.hibernate.properties)
*   `com.h2database:h2:2.1.214` - [h2.hibernate.properties](example/h2.hibernate.properties)
*   `com.microsoft.sqlserver:mssql-jdbc:11.2.3.jre11` - [sqlserver.hibernate.properties](example/sqlserver.hibernate.properties)
*   `com.zaxxer:HikariCP:5.0.1`

需要其他数据库驱动或连接池支持，请添加 `plugin-shared-libraries` 依赖，有2种方法

1.  将 **Jar包** 放到 `plugin-shared-libraries` 目录中一同被 `mirai-console` 加载

2.  在 `plugin-shared-libraries/libraries.txt` 中添加 maven 引用，  
    例如 `com.oracle.database.jdbc:ojdbc11:21.8.0.0`

## 在 Mirai Console Plugin 项目中引用

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-hibernate-plugin:${version}")
}

// hibernate 6 和 HikariCP 5 需要 jdk11
mirai {
    jvmTarget = JavaVersion.VERSION_11
}
```

## 在 Mirai Core Jvm 项目中引用

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("xyz.cssxsh.mirai:mirai-hibernate-plugin:${version}")
}
```
需要手动对 `xyz.cssxsh.mirai.hibernate.factory` 进行初始化，和对 `MiraiHibernateRecorder` 进行注册

**Maven 项目请根据上面的 maven-central 指向的链接查询相关配置方法**

## 在 mirai-api-http 中引用

使用本插件作为 mirai-api-http 的消息源需要额外的拓展插件 <https://github.com/cssxsh/mirai-hibernate-http> 

## 一些方法和类说明

*   [MiraiHibernateConfiguration](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateConfiguration.kt)
    配置的，对应于 `JvmPlugin` 的 `SessionFactory`  
    默认将会读取(生成)在 `config` 目录下的 `hibernate.properties` 作为配置文件  
    并且自动扫描加载当前插件的 `entry` 类包中被 `jakarta.persistence.Entity` 标记的实体类

*   [MiraiHibernateRecorder](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateRecorder.kt)  
    是本插件自带的消息记录器，通过对 `MessageEvent` 和 `MessagePostSendEvent` 记录，保存消息历史到数据库

*   [CriteriaBuilder.rand](src/main/kotlin/xyz/cssxsh/hibernate/Criteria.kt)  
    `MiraiHibernateConfiguration` 中会对 Sqlite / PostgreSql 的 `random` 进行别名注册为 `rand` 统一SQL语句的中的随机函数名

*   [CriteriaBuilder.dice](src/main/kotlin/xyz/cssxsh/hibernate/Criteria.kt)  
    `MiraiHibernateConfiguration` 中会注册名为 `dice` 的宏，用于随机取行

### 示例代码

*   [kotlin](src/test/kotlin/xyz/cssxsh/mirai/test/MiraiHibernatePluginTest.kt)
*   [java](src/test/java/xyz/cssxsh/mirai/test/MiraiHibernateDemo.java)

## 安装

### MCL 指令安装

**请确认 mcl.jar 的版本是 2.1.0+**  
`./mcl --update-package xyz.cssxsh.mirai:mirai-hibernate-plugin --channel maven-stable --type plugins`

### 手动安装

1.  从 [Releases](https://github.com/cssxsh/mirai-hibernate-plugin/releases) 或者 [Maven](https://repo1.maven.org/maven2/xyz/cssxsh/mirai/mirai-hibernate-plugin/) 下载 `mirai2.jar`
2.  将其放入 `plugins` 文件夹中

## [爱发电](https://afdian.net/@cssxsh)

![afdian](.github/afdian.jpg)