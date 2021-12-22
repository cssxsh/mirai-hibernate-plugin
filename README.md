# [Mirai Hibernate Plugin](https://github.com/cssxsh/mirai-hibernate-plugin)

> Mirai Hibernate 前置插件

Mirai-Console的前置插件，用于使用 Hibernate ORM 框架的初始化 

[![maven-central](https://img.shields.io/maven-central/v/xyz.cssxsh.mirai/mirai-hibernate-plugin)](https://search.maven.org/artifact/xyz.cssxsh.mirai/mirai-hibernate-plugin)

## 数据库支持

本插件打包了以下版本的数据库驱动

* MySql `mysql:mysql-connector-java:8.0.26`
* Sqlite `org.xerial:sqlite-jdbc:3.36.0.3`

## 在插件项目中引用

```
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-hibernate-plugin:${version}")
}
```

使用扩展函数 `JvmPlugin.factory` 可以获取通过 [MiraiHibernateConfiguration](src/main/kotlin/xyz/cssxsh/mirai/plugin/MiraiHibernateConfiguration.kt)
配置的，对应于 `JvmPlugin` 的 `SessionFactory`,   
`MiraiHibernateConfiguration` 将会读取(生成)在 `configFolder` 目录下的 `hibernate.properties` 作为配置文件  
并且自动扫描加载 `JvmPlugin` 所在类包下的被 `javax.persistence.Entity` 标记的实体类

### 示例代码

* [kotlin](src/test/kotlin/xyz/cssxsh/mirai/plugin/MiraiHibernatePluginTest.kt)

## 安装

### MCL 指令安装

`./mcl --update-package xyz.cssxsh.mirai:mirai-hibernate-plugin --channel stable --type plugin`

### 手动安装

1. 运行 [Mirai Console](https://github.com/mamoe/mirai-console) 生成`plugins`文件夹
1. 从 [Releases](https://github.com/cssxsh/mirai-hibernate-plugin/releases) 下载`jar`并将其放入`plugins`文件夹中