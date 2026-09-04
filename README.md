# 天机学堂后端

## 项目概述

本项目是 黑马天机学堂 的后端微服务实现，采用 完整springCloudAlibaba 技术栈构建。

- **后端框架**：.SpringCloudAlibaba
- **数据库**：Mysql8.0(mybatis-plus)、Redis
- **缓存与分布式锁**：Redisson(cache-cat)
- **搜索引擎**：Elasticsearch（用于新闻、公告的全文检索）
- **日志**：Logback
- **认证与安全**：JWT（管理端）、ECDSA 签名验证（管理端写操作）、SHA3-256 完整性校验（登录/登出）
- **JSON流处理**：FastJson2

---

## API 接口列表

### 基础信息

---

### 前台公开接口（无需认证）

---

### 管理端接口（需 JWT 认证）

---

### 查看SQL的ER图

访问：

```bash
https://www.drawdb.app/
```

导入根目录的ER.json来访问ER图

---

### Docker 本地依赖环境

本项目通过 Docker Compose 在本地开发环境中运行所有依赖服务（SQL Server、Redis、Elasticsearch）以及后端 API（Admin/User 服务）。

#### 前置条件

已安装以下工具：

- JDK25（用于本地代码调试，非必需）
- 已安装 Docker Desktop（或 Docker Engine + Docker Compose

#### 部署基础设施

1.克隆项目，进入项目根目录（包含 docker-compose.yml 和 schema.sql）。
2.一键启动所有服务：

```bash
docker-compose up -d
```

#### 检查服务状态

```bash
docker-compose ps
```

#### 常用维护命令

操作:

- 启动所有服务（后台）：docker-compose up -d
- 停止所有服务：docker-compose down
- 停止并删除数据卷（重置数据库）：docker-compose down -v
- 查看日志：docker-compose logs -f [服务名]
- 重启某个服务：docker-compose restart [服务名]
- 更新代码后重新构建并启动：docker-compose build [服务名] && docker-compose up -d [服务名]

当前后端配置默认连接：

- Mysql：`127.0.0.1:3306`
- Redis：`localhost:6379`
- Elasticsearch：`http://localhost:9200`

---

#### 重新初始化数据库

如需重置数据库，可删除数据卷并重启：

```bash
docker-compose down -v
docker-compose up -d
```
