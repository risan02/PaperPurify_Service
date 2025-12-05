# 配置文件说明

## 配置文件结构

本项目采用Spring Boot的多环境配置管理，将不同环境的配置分离，便于管理和部署。

### 配置文件列表

1. **application.yml** - 主配置文件（公共配置）
2. **application-dev.yml** - 开发环境配置
3. **application-prod.yml** - 生产环境配置
4. **application-druid.yml** - 已废弃（配置已整合到环境配置文件中）

## 环境切换

### 方式一：修改主配置文件（推荐用于本地开发）

在 `application.yml` 中修改：

```yaml
spring:
  profiles:
    active: dev  # 开发环境
    # active: prod  # 生产环境
```

### 方式二：启动参数（推荐用于生产部署）

```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

### 方式三：环境变量

```bash
# Linux/Mac
export SPRING_PROFILES_ACTIVE=prod

# Windows
set SPRING_PROFILES_ACTIVE=prod
```

## 配置内容说明

### application.yml（主配置）
- 项目基本信息
- 服务器配置
- Token配置
- MyBatis配置
- 其他公共配置

### application-dev.yml（开发环境）
- 数据库配置（本地数据库）
- Redis配置（本地Redis）
- 日志级别（debug）
- 热部署开关（开启）

### application-prod.yml（生产环境）
- 数据库配置（生产数据库）
- Redis配置（生产Redis）
- 日志级别（info）
- 热部署开关（关闭）
- 连接池参数优化

## 注意事项

1. **数据库配置**：生产环境部署前，请务必修改 `application-prod.yml` 中的数据库连接信息
2. **Redis配置**：生产环境建议设置Redis密码
3. **日志级别**：生产环境使用info级别，避免产生过多日志
4. **连接池**：生产环境的连接池参数已优化，可根据实际负载调整

## 迁移说明

如果之前使用 `application-druid.yml`，现在配置已整合到环境配置文件中：
- 数据库配置 → `application-dev.yml` 或 `application-prod.yml`
- Redis配置 → `application-dev.yml` 或 `application-prod.yml`

`application-druid.yml` 文件可以删除或保留作为备份。

