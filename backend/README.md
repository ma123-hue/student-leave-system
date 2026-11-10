# 学生请销假系统后端

这是 Spring Boot + MyBatis-Plus + MySQL 后端项目。项目启动时会读取 `src/main/resources/schema.sql` 和 `src/main/resources/data.sql`，自动创建数据库表并插入测试学生账号。

## 环境要求

- JDK 8
- Maven 3.6+
- MySQL 5.7+ 或 MySQL 8.x

## 数据库配置

默认配置在 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_leave_system?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

通过 `DB_URL`、`DB_USERNAME` 和 `DB_PASSWORD` 环境变量配置本机数据库。`createDatabaseIfNotExist=true` 会在 MySQL 里自动创建 `student_leave_system` 数据库。

## 启动

```bash
mvn spring-boot:run
```

启动成功后后端地址为：

```text
http://localhost:8080
```

## 测试账号

| 学号 | 初始密码 | 说明 |
| --- | --- | --- |
| 2024000001 | 123456 | 首次登录需要修改密码 |
| 2024000002 | 123456 | 首次登录需要修改密码 |

管理端测试账号：`admin001`，初始密码：`123456`。

## 给别人运行时要带哪些文件

把整个 `back` 目录发给别人即可，至少要包含：

- `pom.xml`
- `src/main/java`
- `src/main/resources/application.yml`
- `src/main/resources/schema.sql`
- `src/main/resources/data.sql`

如果只发 Java 代码，不发数据库初始化 SQL，别人本地没有表和测试数据，项目就会启动或登录失败。
