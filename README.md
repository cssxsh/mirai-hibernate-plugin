# [Mirai Hibernate Plugin](https://github.com/cssxsh/mirai-hibernate-plugin)

> Mirai Hibernate 前置插件

[Mirai Console](https://github.com/mamoe/mirai-console) 的前置插件，用于 Hibernate ORM 框架的初始化

[![maven-central](https://img.shields.io/maven-central/v/xyz.cssxsh.mirai/mirai-hibernate-plugin)](https://search.maven.org/artifact/xyz.cssxsh.mirai/mirai-hibernate-plugin)

插件自带聊天记录器 [MiraiHibernateRecorder](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateLoader.kt),  
会记录 `群聊/私聊` 的内容到数据库方便其他插件使用，默认是 `h2database` 数据库(since `2.2.0+`)  
数据库配置在 `config/xyz.cssxsh.mirai.plugin.mirai-hibernate-plugin/hibernate.properties`  
`2.2.0` 之前的版本的 默认数据库 为 `sqlite`, 你可以直接删掉 `hibernate.properties`, 让其重新生成 `h2database` 配置  

## 数据库支持

本插件打包了以下版本的数据库驱动和连接池

* `mysql:mysql-connector-java:8.0.29`
* `org.xerial:sqlite-jdbc:3.36.0.3`
* `org.postgresql:postgresql:42.3.6`
* `com.h2database:h2:2.1.212`
* `com.zaxxer:HikariCP:5.0.1`

需要其他数据库驱动支持请将 **数据库驱动Jar包** 放到 `plugins` 目录中一同被 `mirai-console` 加载

## 在 Mirai Console Plugin 项目中引用

```
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-hibernate-plugin:${version}")
}
```

## 在 Mirai Core Jvm 项目中引用

```
repositories {
    mavenCentral()
}

dependencies {
    implementation("xyz.cssxsh.mirai:mirai-hibernate-plugin:${version}")
}
```
需要手动对 `xyz.cssxsh.mirai.hibernate.factory` 进行初始化，和对 `MiraiHibernateRecorder` 进行注册

## 一些方法和类说明

* [MiraiHibernateConfiguration](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateConfiguration.kt)
  配置的，对应于 `JvmPlugin` 的 `SessionFactory`  
  默认将会读取(生成)在 `configFolder` 目录下的 `hibernate.properties` 作为配置文件  
  并且自动扫描加载 `JvmPlugin` 所在类包下的被 `javax.persistence.Entity` 标记的实体类

* [MiraiHibernateRecorder](src/main/kotlin/xyz/cssxsh/mirai/hibernate/MiraiHibernateRecorder.kt)  
  是本插件自带的消息记录器，通过对 `MessageEvent` 和 `MessagePostSendEvent` 记录，保存消息历史到数据库

* [CriteriaBuilder.rand](src/main/kotlin/xyz/cssxsh/hibernate/Criteria.kt)  
  `MiraiHibernateConfiguration` 中会对 Sqlite 的 `random` 进行别名注册为 `rand` 统一SQL语句的中的随机函数名

### 示例代码

* [kotlin](src/test/kotlin/xyz/cssxsh/mirai/test/MiraiHibernatePluginTest.kt)

## 安装

### MCL 指令安装

`./mcl --update-package xyz.cssxsh.mirai:mirai-hibernate-plugin --channel stable --type plugin`

### 手动安装

1. 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
1. 从 [Releases](https://github.com/cssxsh/mirai-hibernate-plugin/releases) 下载`jar`并将其放入`plugins`文件夹中