# 天机学堂后端

## 项目概述

本项目是 黑马天机学堂 的后端微服务实现，采用 完整springCloudAlibaba 技术栈构建，并且使用HuTool工具包。

- **后端框架**：.SpringCloudAlibaba
- **数据库**：Mysql8.0(mybatis-plus)、cancl(实现数据库与搜索引擎同步)、Redis、MongoDB
- **缓存与分布式锁**：caffeine、Redisson
- **搜索引擎**：Elasticsearch（用于新闻、公告的全文检索）
- **全局日志**：GrayLog2
- **链路追踪**：SykWalking
- **认证与安全**：sa-Token（管理端）、ECDSA 签名验证（管理端写操作）、SHA3-256 完整性校验（登录/登出）
- **JSON流处理**：FastJson2
- **聚合支付(微信、支付宝)**：IJPay
---

## 模块介绍
- tj-api：约定服务(存放DTO等)
- tj-auth：权限服务(使用sa-token鉴权)
- tj-common：通用工程
- tj-message：消息中心
- tj-gateway：网关
- tj-user：用户服务
- tj-pay：支付服务
- tj-course：课程服务
- tj-exam：考试服务
- tj-search：搜索服务
- tj-trade：交易服务
- tj-learning：学习服务
- tj-promotion：促销服务(提供优惠券)
- tj-media：媒资服务(存放视频)
- tj-data：数据服务
- tj-remark：评价服务
---

## API 接口列表(以模块区分)

### 基础信息

---

### 前台公开接口（无需认证）

---

### 管理端接口（需 JWT 认证）

---

### 查看SQL的ER图

打开db/ER访问：

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

1.克隆项目，进入项目根目录/docker
2.一键启动所有服务：

```bash
docker-compose up -d
```

#### 检查服务状态

```bash
docker-compose ps -a
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

- Mysql：`localhost:3306`
- cancl：`localhost:11111`
- powerjob-mysql：`localhost:3307`
- Redis：`localhost:6379`
- Elasticsearch：`localhost:9200`
- skywalking-oap：`localhost:11800`、web：`localhost:12800`
- skywalking-ui：`localhost:8088`
- nacos：`localhost:8848`
- seata-server：`8091`、web：`localhost:7091`
- sentinel：`localhost:8858`
- rabbitmq：`localhost:5672`、web：`localhost:15672`
- powerjob：`localhost:7700`
- graylog：`localhost:9000`
- nginx：`localhost:8080`

---

#### 重新初始化数据库

如需重置数据库，可删除数据卷并重启：

```bash
docker-compose down -v
docker-compose up -d
```
