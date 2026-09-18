# 天机学堂后端（TJ_Project_Api）

黑马天机学堂的后端微服务实现，基于 **Spring Boot 4.1.1 + Spring Cloud 2025.1.3 + Spring Cloud Alibaba 2025.1.0.0 + Java 25**。

- **统一包名**：`com.tjxt.*`（Maven groupId 同为 `com.tjxt`）
- **服务间调用**：gRPC（`spring-boot-starter-grpc-client/server` + protobuf 代码生成）
- **认证鉴权**：Spring Security 7 + JWT（RS256，java-jwt）
- **网关**：Spring Cloud Gateway（WebFlux）+ 响应式 Security
- **数据库**：MySQL 8（MyBatis-Plus）、Redis（缓存/分布式锁/权限表）、Elasticsearch（Canal 同步）
- **消息**：Spring Cloud Stream + RabbitMQ（替代老项目 spring-amqp）
- **分布式事务**：Seata 2.5.0（`org.apache.seata`）
- **定时任务**：PowerJob（替代老项目 xxl-job）
- **文档**：Knife4j（服务端）+ knife4j-gateway（网关聚合）
- **可观测**：SkyWalking + Graylog + Logback/SLF4J、Actuator、Sentinel

---

## 一、模块与端口总览

### 1.1 业务服务（HTTP / gRPC）

> gRPC 端口约定 = **HTTP 端口 + 1000**（避免同机多服务抢默认 9090）。
> 服务名与 Nacos 配置文件名、gRPC 通道名三者一致（如 `user-service`）。

| 服务名 | HTTP 端口 | gRPC 端口 | 数据库 | 包名（Swagger 扫描包） |
|---|---|---|---|---|
| `gateway-service` | **10010** | 不启动 gRPC 服务端 | — | — |
| `auth-service` | **10011** | **11011** | `tj_auth` | `com.tjxt.tjauth` |
| `user-service` | **10012** | **11012** | `tj_user` | `com.tjxt.tjuser` |
| `course-service` | **10013** | **11013** | `tj_course` | `com.tjxt.tjcourse` |
| `data-service` | **10014** | **11014** | `tj_data` | `com.tjxt.tjdata` |
| `exam-service` | **10015** | **11015** | `tj_exam` | `com.tjxt.tjxeam` |
| `learning-service` | **10016** | **11016** | `tj_learning` | `com.tjxt.tjlearning` |
| `media-service` | **10017** | **11017** | `tj_media` | `com.tjxt.tjmedia` |
| `message-service` | **10018** | **11018** | `tj_message` | `com.tjxt.messageservice` |
| `pay-service` | **10019** | **11019** | `tj_pay` | `com.tjxt.payservice` |
| `trade-service` | **10020** | **11020** | `tj_trade` | `com.tjxt.tjtrade` |
| `search-service` | **10021** | **11021** | `tj_search` | `com.tjxt.tjsearch` |
| `remark-service` | **10022** | **11022** | `tj_remark` | `com.tjxt.tjremark` |
| `promotion-service` | **10023** | **11023** | `tj_promotion` | `com.tjxt.tjpromotion` |

以上端口都由 Nacos 的 `*-service.yaml` 下发（`server.port` 与 `spring.grpc.server.port`），也可用环境变量覆盖：
`TJ_GRPC_USER_PORT`、`TJ_GRPC_COURSE_PORT` … 等（见 `docker/nacos/*-service.yaml`）。

### 1.2 基础设施（docker-compose）

| 组件 | 地址 | 说明 |
|---|---|---|
| MySQL 8 | `localhost:3306` | root/root，业务库 + Nacos + Seata + Canal |
| MySQL（PowerJob 专用） | `localhost:3307` | powerjob-daily 库 |
| Redis 7 | `localhost:6379` | 缓存、分布式锁、**权限表**（`auth:privileges`） |
| RabbitMQ | `localhost:5672`（管理台 `15672`） | admin/admin123 |
| Elasticsearch | `localhost:9200` | 搜索服务 + SkyWalking/Graylog 存储 |
| Nacos 3.1.1 | `localhost:8848`（控制台 `localhost:8849`，gRPC `9848`/`9849`） | nacos/nacos |
| Seata Server | `localhost:8091`（控制台 `7091`） | 分布式事务 |
| PowerJob Server | `localhost:7700` | 调度中心 |
| Sentinel Dashboard | `localhost:8858` | sentinel/sentinel |
| Canal Server / Adapter | `localhost:11111` | MySQL → ES 同步 |
| SkyWalking OAP / UI | `11800`、`12800` / `localhost:8088` | 链路追踪 |
| Graylog | `localhost:9000` | 全局日志 |
| Nginx Proxy Manager | `8080`、`443`、`81` | 反向代理 |

> **所有服务的端口（10010-10023 HTTP、11011-11023 gRPC）都需要空闲**；如果本机上有别的程序占用，服务会启动失败。

---

## 二、模块介绍

| 模块 | 说明                                                                                                                                                                                                              |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `tj-common` | 通用工程：统一响应 `R<T>`、全局异常、Fastjson2、MyBatis-Plus 插件与审计字段填充、Redisson/`@Lock`、MQ 助手、Knife4j、虚拟线程、认证协议常量（`AuthConstants`/`RoleUtils`）                                        |
| `tj-microservice-sdk` | 共享契约：9 个 `.proto` + 8 个 `*GrpcClient`、`UserContext`/`UserContextFilter`、gRPC 双端用户透传、`RoleCache`/`CategoryCache`、各服务 DTO                                                                       |
| `tj-gateway` | 网关：JWT 验签（JWKS）、白名单、**路径级鉴权**（权限表）、`user-info` 透传、401/403 统一 JSON、文档聚合。是所有外部请求的统一门卫：在请求进入微服务前验 Token、按路径做一次粗粒度角色拦截、清洗并注入可信用户信息 |
| `tj-auth` | 认证服务：登录/刷新/登出（RS256 签发）、`/jwks` 公钥、角色-权限-菜单维护、权限表写 Redis。是身份与权限数据的权威中心：验证账号密码、签发/刷新/注销 Token、保管 RSA 私钥、提供公钥、维护“接口 → 可访问角色”的权限数据                                                                                                                        |
| `tj-user` | 用户服务（账号、用户信息）                                                                                                                                                                                        |
| `tj-course` | 课程服务（分类、课程、目录、章节）                                                                                                                                                                                |
| `tj-learning` | 学习服务（课表、学习记录）                                                                                                                                                                                        |
| `tj-exam` | 考试服务（题目、考试记录）                                                                                                                                                                                        |
| `tj-search` | 搜索服务（ES 检索）                                                                                                                                                                                               |
| `tj-trade` | 交易服务（下单、订单）                                                                                                                                                                                            |
| `tj-pay` | 支付服务（IJPay：支付宝/微信）                                                                                                                                                                                    |
| `tj-promotion` | 促销服务（优惠券）                                                                                                                                                                                                |
| `tj-media` | 媒资服务（视频/文件，阿里云 OSS；腾讯云 COS/VOD 待引入）                                                                                                                                                          |
| `tj-message` | 消息中心（短信、站内信）                                                                                                                                                                                          |
| `tj-remark` | 评价/点赞服务                                                                                                                                                                                                     |
| `tj-data` | 数据服务（统计）                                                                                                                                                                                                  |

---

## 三、快速开始（本地 IDE 运行）

1. **启动基础设施**（见第五节 Docker 命令），确认 MySQL / Redis / RabbitMQ / ES / Nacos 起来。
2. **发布 Nacos 配置**：`docker/nacos/` 下的 yaml 通过一次性容器 `tj-nacos-init` 自动发布，也可以手动执行
   `docker/nacos/import-nacos.ps1`（把 `*-service.yaml`、`shared-*.yaml` 全部推送到 Nacos 的 `DEFAULT_GROUP`）。
3. **启动顺序**（重要）：
   ```
   基础设施 → Nacos 配置发布 → auth-service(10011) → gateway-service(10010) → 其它业务服务
   ```
   网关需要通过服务发现拿到 auth 的 `/jwks` 公钥；auth 没起来时网关验签会返回 40100（认证服务暂不可用）。
4. **验证**：
   ```bash
   # 公开接口（白名单）：应返回标准 JWK Set
   curl http://localhost:10010/auth/jwks

   # 登录拿 token
   curl -X POST http://localhost:10010/auth/accounts/login \
        -H 'Content-Type: application/json' \
        -d '{"cellPhone":"13800000000","password":"123456"}'

   # 带 token 访问受保护接口 → 200
   curl -i http://localhost:10010/user/users/1 -H "authorization: <access token>"

   # 不带 token → 401，响应体是 R JSON
   curl -i http://localhost:10010/user/users/1
   ```
   > 注意：**必须走网关**（10010）。直连服务端口没有 `user-info` 头，等价于未登录。

---

## 四、认证与鉴权速查

| 环节 | 位置 | 关键点 |
|---|---|---|
| 登录/刷新/登出 | `auth-service` | access token（默认 30m）放响应体；refresh token（默认 7d）放 HttpOnly Cookie；refresh 的 JTI 存 Redis，登出即失效 |
| 验签 | `gateway-service` | 从 auth 的 `/jwks` 拉公钥（10 分钟刷新），RS256 校验；`refresh token` 不能当 access 用（带 `tokenType` 声明） |
| 白名单 | 网关 `tj.auth.exclude-path` | 写**网关侧路径**（路由带前缀 + `StripPrefix=1`）：`/auth/accounts/login` 而不是 `/accounts/login`；支持 `POST:/path` 写法 |
| 路径级鉴权 | 网关 `PrivilegeAuthorizationManager` | 读 Redis `auth:privileges`（field=`METHOD:/uri`，value 含允许的 roleId 集合）+ `version` 版本号；未配置权限的路径"登录即可访问" |
| 用户信息透传 | 网关 `UserInfoRelayFilter` | 注入 `user-info = Base64(JSON(LoginUserDTO))`，并**剥离客户端伪造的同名头** |
| 下游鉴权 | `tj-microservice-sdk` | `UserContextFilter` 解 `user-info` → SecurityContext（`ROLE_STAFF/STUDENT/TEACHER`、`ROLE_<roleName>`）+ `UserContext`（gRPC 透传），可用 `@PreAuthorize` |
| 服务自身鉴权 | `auth-service` | `SecurityConfig`（无状态+白名单）+ `AuthCheckAspect`（动态"路径→角色"，数据在 auth 库） |

完整流程、配置项、排查手册见 **[`docs/authentication.md`](docs/authentication.md)**。

---

## 五、gRPC 通信

- **客户端**：通道名 = 目标服务名，由 `GrpcClientRegistrationAutoConfiguration` 用
  `@ImportGrpcClients(target = "user-service", types = UserServiceGrpc.UserServiceBlockingStub.class)` 显式绑定；
  地址在 Nacos `shared-spring.yaml` 统一配置：
  ```yaml
  spring:
    grpc:
      client:
        channel:
          user-service:
            target: ${TJ_GRPC_USER_TARGET:static://127.0.0.1:11012}
  ```
- **服务端**：各服务在 `*-service.yaml` 配 `spring.grpc.server.port`（= HTTP + 1000）。
- **启动自检**：`GrpcChannelHealthLogger` 会打印 gRPC 服务端端口和每个通道解析到的地址；若某通道没配地址（退回默认
  `static://localhost:9090`）会打 WARN 并给出配置样例。
- **用户信息透传**：`UserRelayClientInterceptor`/`UserRelayServerInterceptor` 通过 gRPC Metadata 的 `user-info`
  传递登录用户（明文 JSON，与 HTTP 头的 Base64 格式不同）。
- **Docker 部署**：用环境变量把地址换成容器名，例如 `TJ_GRPC_USER_TARGET=static://tj-user:11012`
  （完整变量清单与 compose 示例见第十节「把业务服务也放进 Docker」）。

---

## 六、Nacos 配置说明（`docker/nacos/`）

| 配置文件 | 作用 |
|---|---|
| `shared-spring.yaml` | 所有服务共享：优雅停机、**接口文档开关（`tj.swagger.*`）**、**gRPC 客户端通道地址** |
| `shared-redis.yaml` | Redis 连接 + lettuce 连接池（需要 `commons-pool2` 依赖才生效） |
| `shared-rabbitmq.yaml` | RabbitMQ 连接 + Spring Cloud Stream（binder=rabbit、延迟交换机、消费失败自动 DLQ） |
| `shared-logs.yaml` | 日志级别（`com.tjxt: debug`）、控制台/文件格式（含 `requestId`） |
| `<服务名>.yaml` | 各服务专属：端口、数据源、Seata、PowerJob、MQ 绑定、Swagger 扫描包、白名单等 |
| `gateway-service.yaml` | 网关路由（`/auth/**`、`/user/**` … 带 `StripPrefix=1`）、CORS、`tj.auth.*` 鉴权配置、knife4j 聚合 |
| `auth-service.yaml` | 认证服务：数据源、密钥路径、issuer、token 有效期、白名单 |

**优先级**：`<服务名>.yaml`（Nacos） > `shared-*.yaml`（Nacos） > 各模块 `src/main/resources/application.yaml`（本地默认值）。
所以本地默认值只是为了"没连 Nacos 也能起"，线上以 Nacos 为准。

---

## 七、注意事项（务必读）

### 7.1 JWT 密钥（RSA）

- **现在可以直接跑**：仓库里已有开发密钥 `tj-auth/src/main/resources/keys/private_key.pem`（PKCS#8 私钥）与
  `public_key.pem`（X.509 公钥），与 Nacos `auth-service.yaml` 的 `classpath:keys/private_key.pem` /
  `public_key.pem` 完全对应，**不需要再生成 `.key`/`.jks` 之类的文件**。
- 生产环境请**替换掉仓库里的开发密钥**（Docker 挂载 + `file:/app/keys/private_key.pem`，配置见 `auth-service.yaml`
  注释）：
  ```bash
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
  openssl rsa -in private_key.pem -pubout -out public_key.pem
  ```
- **公私钥必须成对**。只换一半会导致网关一直 401「无效的 token」。
- 网关不需要私钥，默认从 auth 的 `/jwks` 拉公钥；离线调试才配 `tj.auth.jwt.public-key-path`。
- 轮换：替换 auth 密钥 → 重启 auth → 网关最多 10 分钟自动刷新（验签失败时也会立即重试一次）。
- 签名算法**固定 RS256**（`java-jwt` 的 `RSA256`），配置项 `tj.auth.jwt.algorithm` 已移除，改算法要同时改
  `JwtIssuer` 与网关 `JwtTokenVerifier`。
- `RsaKeyLoader` 有兜底：配置的路径取不到会用内置 `classpath:keys/*.pem` 并打 WARN —— 路径写错不会崩，但会告警。

### 7.2 必须走网关

业务服务默认 `anyRequest().authenticated()`，用户身份完全来自网关注入的 `user-info` 头。**直连服务端口等于未登录**，
请把业务端口限制在内网。

### 7.3 白名单与路由前缀

网关路由带前缀 + `StripPrefix=1`（`/auth/**` → auth 的 `/accounts/**`）。加免认证接口时：

- 网关 `tj.auth.exclude-path` 写**网关路径**：`/auth/accounts/login`；
- auth 自己的白名单（`auth-service.yaml`）写**后端路径**：`/accounts/login`。
- 网关还有代码兜底白名单（`AuthProperties.DEFAULT_EXCLUDE_PATH`，与配置取并集），Nacos 漏配也不会把登录接口拦掉。

### 7.4 权限表（路径级鉴权）

- Redis：Hash `auth:privileges`（field=`METHOD:/uri`，value=`PrivilegeRoleDTO` JSON），String `version` 版本号。
- 网关匹配时**同时**用"网关原始路径"和"去掉首段前缀的路径"，所以权限表里登记网关路径或后端路径都能命中。
- 未配置权限 = 登录即可访问；`roles` 为空 = 不限制角色；角色比对用的是 **roleId**。
- Redis 不可用时网关保留上次快照并告警，快照为空则"只认证不鉴权"（`tj.auth.privilege.enabled=false` 可显式关闭）。
- 权限改动后最多 20 秒（`refresh-interval`）生效；关注网关日志「网关权限缓存已刷新：版本 X，共 N 条规则」。

### 7.5 Redis / Redisson / 连接池

- 只要配置了 `spring.data.redis.host`（`shared-redis.yaml` 提供），`tj-common` 的 `RedissonConfig` 就会创建
  `RedissonClient` → 分布式锁 `@Lock` 可用；**Redis 不可达时该服务启动失败**（与老项目一致）。
- `lettuce.pool.*` 需要 `commons-pool2` 才生效（缺了不报错但静默不池化），本项目相关服务都已声明。
- `tj-media` 显式排除了 `spring-data-redis`/`redisson`（不需要 Redis）。

### 7.6 动态表名（默认关闭）

- `tj.mybatis.dynamic-table.enabled` 默认 **false**：表名按实体/注解原样使用（正常行为）。
- 老代码在取不到年份时会**随机**给表名加 `_2018`/`_2019`，等于所有 SQL 都查错表；如确实按年份分表，请显式开启：
  ```yaml
  tj:
    mybatis:
      dynamic-table:
        enabled: true
        default-suffix: _2024     # TableNameContext 没设年份时用它（不再随机）
  ```
- 想自定义：直接声明自己的 `DynamicTableNameInnerInterceptor`/`MybatisPlusInterceptor` Bean（有 `@ConditionalOnMissingBean`）。

### 7.7 审计字段自动填充

`MyBatisAutoFillHandler`（由 `MybatisPlusConfig` 注册）会填充 `creater/updater/createTime/updateTime`，
用户 ID 取自 `UserContext`（未登录时不填用户字段）。实体字段上需要 `@TableField(fill = ...)` 才能生效。

### 7.8 PowerJob（定时任务）

- 只有显式配置了 `powerjob.worker.app-name` 的服务才会启用 Worker（避免只引依赖、没接调度中心的服务启动报错）。
- 需要 PowerJob Server（`localhost:7700`）与其专用 MySQL（`3307`）；已配置的服务带
  `allow-lazy-connect-server: true`，Server 未就绪也能启动（会重试）。
- 老的 xxl-job 配置已全部替换，不要再引 `xxl-job-core`。

### 7.9 接口文档

- 开关在 `shared-spring.yaml`：`tj.swagger.enable: true` + 各服务 `tj.swagger.package-path`（Controller 所在包）。
- `auth-service` 只暴露 `/v3/api-docs`（供网关聚合），关闭了 UI；需要界面就把 `knife4j.enable` 与
  `springdoc.swagger-ui.enabled` 改成 `true`。
- 网关用 `knife4j.gateway`（服务发现聚合）访问 `http://localhost:10010/doc.html`。

### 7.10 数据库（13 个业务库 / 59 张表）与表结构来源

`db/schema.sql` 是从课程原始导出的 SQL 复制过来并整理过的，**原来只包含 9 个库**
（`tj_auth/tj_course/tj_exam/tj_media/tj_message/tj_pay/tj_search/tj_trade/tj_user`，45 张表 + 种子数据），
而项目里有 13 个服务需要数据库。已补齐（现共 13 个库 / 59 张表）：

| 库 | 来源 | 说明 |
|---|---|---|
| 上述 9 个库 | 课程原始导出 | 表结构 + **种子数据**（用户、角色、权限、菜单、课程等）都保留 |
| `tj_learning`（9 张表） | 依据**老项目实体**（`tianji/tj-learning/.../domain/po/*` 的 `@TableName`）重建 | `learning_lesson`、`learning_record`、`note`、`note_user`、`points_board`、`points_board_season`、`points_record`、`interaction_question`、`interaction_reply` |
| `tj_promotion`（4 张表） | 同上（老 `tianji/tj-promotion`） | `coupon`、`coupon_scope`、`exchange_code`、`user_coupon` |
| `tj_remark`（1 张表） | 同上（老 `tianji/tj-remark`） | `liked_record` |
| `tj_data` | 老数据服务是 Redis 实现（无 `@TableName` 实体） | 只建空库，保证服务能启动 |

> ⚠️ 后 4 个库**只有结构、没有种子数据**，且表结构是按老项目实体（字段名/类型/注释）反推的：
> 字段长度、索引、唯一约束可能与课程官方 DDL 有差异。若你拿到官方 DDL，直接用官方版本替换这一段即可
> （替换时保留 `CREATE DATABASE`/`USE` 语句）。重建的 DDL 在 `db/schema.sql` 中带 `-- 说明：` 注释块标注。

**各服务的数据源**：13 个服务都在自己的 `*-service.yaml` 里配了 `spring.datasource.*`（库名与上表一致）；
`gateway-service` 不需要数据库（已排除 MyBatis）。所以"某个服务起不来、报
`Failed to determine a suitable driver class`"时，先检查这个服务的 Nacos 配置里有没有 `spring.datasource.url`。

### 7.11 库模块不要放 `application.yaml`

`message-api`、`message-domain`、`pay-api`、`pay-domain` 是**库模块**（会作为 jar 被 `user-service`/`trade-service`
依赖）。它们原来带的 `application.yaml` 会污染消费方配置（服务名、gRPC 端口等），已删除；**不要再加回去**，
部署参数统一放各自可部署服务的 Nacos 配置里。

### 7.12 测试

- 纯单元测试（不依赖 Nacos/MySQL/Redis）：`mvn -pl tj-common,tj-microservice-sdk,tj-gateway,tj-auth test`
  （覆盖 JWT 签发/验签、白名单匹配、权限表匹配、gRPC 通道绑定，共 25 个）。
- 各服务里 IDE 生成的 `*ApplicationTests`（`@SpringBootTest`）需要真实基础设施，全量构建请用
  `mvn clean install -DskipTests`（会编译测试代码但不执行）。

### 7.13 其它

- **不要用 Java 25 的实例 main（`void main`）写启动类**：Spring Boot `repackage` 会报 `Unable to find main class`，
  必须 `public static void main`。
- **自动配置里的 Bean 名不能重复**：同名 `@Bean` 会抛 `BeanDefinitionOverrideException` 让服务起不来
  （历史上 `MvcConfig`/`Fastjson2Config` 都注册过 `fastJsonHttpMessageConverter`）。
- **库内包不在业务服务扫描范围**：`tj-common`/`tj-microservice-sdk` 的组件必须登记在
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 里才会生效。
- **本地 `application.yaml` 只写默认值**：Nacos 会覆盖；新增共享配置优先放 `shared-*.yaml`。
- 老项目的 Feign 已全部替换为 gRPC；XXL-Job → PowerJob；spring-amqp → Spring Cloud Stream；hutool-jwt → java-jwt；
  springfox → Knife4j(OpenAPI3)。迁移对照表见 [`docs/dependency-comparison.md`](docs/dependency-comparison.md)。

---

## 八、构建与测试

```bash
# 全量构建（跳过测试执行，编译测试代码 + 打包）
mvn clean install -DskipTests

# 只构建某模块及其依赖
mvn -pl tj-gateway -am install -DskipTests

# 单元测试（网关/认证/公共/SDK）
mvn -pl tj-common,tj-microservice-sdk,tj-gateway,tj-auth test
```

---

## 九、文档索引

| 文档 | 内容 |
|---|---|
| [`docs/authentication.md`](docs/authentication.md) | 认证鉴权说明：职责分工、端到端流程、Token 声明、配置项、白名单与路由前缀、权限表匹配规则、排查手册 |
| [`docs/dependency-comparison.md`](docs/dependency-comparison.md) | 新老项目依赖能力对比、破坏性问题清单与**修复记录**、gRPC 端口约定、附录（模块名映射 / Boot 4 artifactId 变化） |

---

## 十、Docker 本地依赖环境

本项目通过 Docker Compose 在本地运行依赖服务（MySQL、Redis、Elasticsearch、RabbitMQ、Nacos、Seata、PowerJob、
Canal、Sentinel、SkyWalking、Graylog、Nginx）；**业务微服务当前默认在宿主机（IDE 或 jar）运行**，端口见第一节。

#### 前置条件

- JDK 25（本地调试用）
- Docker Desktop（或 Docker Engine + Docker Compose）

#### 启动

```bash
cd docker
docker-compose up -d          # 基础设施 + Nacos 配置发布任务（tj-nacos-init）
docker-compose ps -a          # 查看状态
```

#### 常用维护命令

- 启动所有服务（后台）：`docker-compose up -d`
- 停止所有服务：`docker-compose down`
- 停止并删除数据卷（重置数据库）：`docker-compose down -v`
- 查看日志：`docker-compose logs -f [服务名]`
- 重启某个服务：`docker-compose restart [服务名]`

#### 把业务服务也放进 Docker（环境变量清单）

服务的端口、gRPC 通道地址、数据源地址都写成 `${环境变量:默认值}` 的形式（如
`${TJ_GRPC_AUTH_TARGET:static://127.0.0.1:11011}`）。**默认值对应"服务在宿主机跑、基础设施在 Docker"** 的现状，
所以现在什么都不用配；一旦把业务服务也放进 compose（同一个 `tj-spring` 网络），容器里的 `127.0.0.1` 指的是容器自己，
就必须用 `environment:` 覆盖这些变量（也可用 `.env` / `env_file`）：

```yaml
services:
  tj-auth:
    image: tjxt/tj-auth:0.0.1
    environment:
      NACOS_SERVER: tj-nacos:8848            # 容器网络里的 Nacos；宿主机跑时不传，默认 127.0.0.1:8848
      TJ_MYSQL_HOST: tj-mysql
      TJ_REDIS_HOST: tj-redis
      TJ_GRPC_AUTH_PORT: 11011               # 自己监听的 gRPC 端口
    ports: ["10011:10011", "11011:11011"]    # 只有宿主机/外部要访问时才需要映射

  tj-user:
    image: tjxt/tj-user:0.0.1
    environment:
      NACOS_SERVER: tj-nacos:8848
      TJ_MYSQL_HOST: tj-mysql
      TJ_REDIS_HOST: tj-redis
      TJ_RABBITMQ_HOST: tj-rabbitmq
      TJ_GRPC_USER_PORT: 11012
      # 要调用的下游：通道名 = 目标服务名，static:// 前缀必须保留
      TJ_GRPC_AUTH_TARGET: static://tj-auth:11011
      TJ_GRPC_COURSE_TARGET: static://tj-course:11013
      TJ_GRPC_LEARNING_TARGET: static://tj-learning:11016
      TJ_GRPC_EXAM_TARGET: static://tj-exam:11015
      # …需要哪个下游就加哪个（共 8 个通道，见第五节）
    ports: ["10012:10012", "11012:11012"]
```

要点：

1. **用 compose 服务名，不要用 `container_name`**：`tj-user` 是网络 DNS 别名，改名容器不影响它。
2. **`NACOS_SERVER` 管 Nacos 地址**：14 个服务的 `application.yaml` 默认段统一写成
   `spring.cloud.nacos.server-addr: ${NACOS_SERVER:127.0.0.1:8848}` —— 不传变量就是 `127.0.0.1:8848`
   （基础设施在 Docker、服务在宿主机），传 `NACOS_SERVER=tj-nacos:8848` 就切到容器网络。
   **已不再依赖 `local` profile**（原来第二段配置里写死的 `tj-nacos:8848` 已删除，避免"profile 段优先级更高、
   把环境变量盖掉"的坑）；`local` 这个 profile 现在是空档，可留给真正的本地开发覆盖用。
3. **两类 gRPC 变量别混**：`TJ_GRPC_<自己>_PORT` = 我监听哪个端口；`TJ_GRPC_<下游>_TARGET` = 我连谁
   （客户端只有 8 个通道：auth / user / course / learning / exam / promotion / remark / trade）。
4. **端口映射**：同网络内互调**不需要** `ports`；只有宿主机或外部要访问时才映射
   （HTTP 10010-10023、gRPC 11011-11023）。
5. **密钥不用塞环境变量**：`TJ_JWT_PRIVATE_KEY` / `TJ_JWT_PUBLIC_KEY` 只出现在 `auth-service.yaml` 注释掉的
   "生产挂载密钥"写法里；容器里挂文件 + `file:/app/keys/private_key.pem`，或者继续用 classpath 内置开发密钥都行。
6. **优先级**：命令行 `--xxx` / `-D` > **环境变量** > Nacos 配置 > 本地 `application.yaml`。
   也就是说，无论 `${TJ_*}` 写在哪个配置源里，最终都从环境变量取值（有就覆盖默认值）。
7. **所有服务相同的变量只写一处**：用 compose 的 YAML 锚点或 `.env` / `env_file`：

   ```yaml
   x-common-env: &common-env          # 只定义一次
     NACOS_SERVER: tj-nacos:8848
     TJ_MYSQL_HOST: tj-mysql
     TJ_REDIS_HOST: tj-redis
     TJ_RABBITMQ_HOST: tj-rabbitmq

   services:
     tj-user:
       environment:
         <<: *common-env              # 展开公共变量
         TJ_GRPC_USER_PORT: 11012
         TJ_GRPC_AUTH_TARGET: static://tj-auth:11011
   ```

其余可直接复用的变量（同一套约定）：`NACOS_SERVER`、`TJ_MYSQL_HOST/PORT/USERNAME/PASSWORD`、`TJ_REDIS_HOST/PORT`、
`TJ_RABBITMQ_HOST/PORT/USERNAME/PASSWORD`、`TJ_POWERJOB_HOST/PORT`、`TJ_ELASTICSEARCH_URIS`、
`TJ_GRPC_<服务名>_PORT`、`TJ_GRPC_<服务名>_TARGET`。

#### 重新初始化数据库

数据库结构由 `db/` 下的脚本初始化，**仅在数据卷为空（首次启动）时执行一次**：

- `db/schema.sql`：**13 个业务库** —— `tj_auth`、`tj_course`、`tj_exam`、`tj_media`、`tj_message`、`tj_pay`、
  `tj_search`、`tj_trade`、`tj_user`（含种子数据），以及 `tj_learning`、`tj_promotion`、`tj_remark`、`tj_data`
  （结构由老项目实体重建，无种子数据，见 7.10）。其中 `tj_course`/`tj_exam`/`tj_trade` 含 Seata 的 `undo_log` 表
- `db/nacos-seata.sql`：`nacos`（Nacos 3.1.1 schema + 默认管理员 nacos/nacos）与 `seata`（事务表）

> 表结构变更需要在运行中的 MySQL 里手动补 DDL，改 SQL 文件不会自动生效；重置用 `docker-compose down -v` 后再 `up -d`。

---

## 十一、查看 SQL 的 ER 图

`db/ER/` 下是本项目的 **DBML** 模型文件（`TJXT_*.dbml`）。打开 <https://www.drawdb.app/> 或
<https://dbdiagram.io/>，导入该 DBML 即可查看/编辑 ER 图。

当前 ER 图已与 `db/schema.sql` **保持一致**：**57 张表 / 649 个字段 / 79 条关系**，覆盖 13 个库
（含本次补齐的 `tj_learning` 9 张、`tj_promotion` 4 张、`tj_remark` 1 张、`tj_data` 空库、以及原导出里漏掉的
课程草稿表与 `message_template`）。

> 两点说明：
> 1. **DBML 没有"库"的概念**，所以多个库里的同名表（如 `tj_course`/`tj_exam`/`tj_trade` 都有的 `undo_log`）只画一次，
>    文件中用 `// tj_xxx：...` 注释标出归属，便于对照。
> 2. 仓库里**没有** `ER.json`（历史 README 写错了），请使用上面的 `.dbml` 文件。
