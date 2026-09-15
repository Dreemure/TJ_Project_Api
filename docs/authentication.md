# 认证与鉴权说明（gateway / auth / microservice-sdk）

> 项目：`TJ_Project_Api`（Spring Boot 4.1.1 + Spring Cloud 2025.1.3 + Spring Security 7.1 + Java 25）
> 统一包名前缀：`com.tjxt.*`
> 本文说明"请求是怎么被认证与鉴权的、配置写在哪里、出问题怎么查"，与代码注释互为补充。

---

## 1. 职责分工（Spring Security 视角）

| 层次 | 模块 | 关键 Spring Security 组件 | 职责 |
|---|---|---|---|
| 边缘（网关） | `tj-gateway` | `SecurityWebFilterChain`（`GatewaySecurityConfig`）、`JwtAuthenticationFilter`、`ReactiveAuthorizationManager`（`PrivilegeAuthorizationManager`） | ① 白名单放行 ② JWT 验签（认证）③ 权限表校验"方法+路径 → 角色"（粗粒度鉴权）④ 注入 `user-info` 供下游消费 ⑤ 401/403 统一 JSON |
| 认证服务 | `tj-auth` | `SecurityFilterChain`（`SecurityConfig`）、`JwtAuthenticationFilter` | ① 登录/刷新/登出（签发 token）② 保护 auth 自身的管理接口 ③ 动态"路径→角色"权限表的维护与缓存（`AuthUtils` + `AuthCheckAspect`） |
| 业务服务 | `tj-microservice-sdk` | `UserContextAutoConfiguration` 提供的默认 `SecurityFilterChain` + `UserContextFilter` | ① 解析网关注入的 `user-info` 写入 `SecurityContext`/`UserContext` ② 提供 `ROLE_*` 权限供 `@PreAuthorize` 使用 ③ gRPC 调用透传用户信息 |

**分层原则**：网关只做"认证 + 粗粒度路径/角色鉴权"，数据级、业务规则的细粒度鉴权留在业务服务（`@PreAuthorize`），避免把业务规则堆到网关。

---

## 2. 端到端流程

### 2.1 登录

```
前端 POST /auth/accounts/login  ──►  网关（白名单放行，不校验 token）
                                  └─►  StripPrefix=1 转发到 auth-service 的 /accounts/login
auth-service: AccountServiceImpl
   1. 通过 gRPC 调 user-service 校验账号密码
   2. JwtIssuer 用 RSA 私钥签发 access token（30m）+ refresh token（7d，JTI 存 Redis）
   3. refresh token 写入 HttpOnly Cookie（refresh / admin-refresh）
   4. 返回 access token（响应体），前端存 localStorage 并放进 Authorization 头
```

### 2.2 带 token 访问业务接口

```
请求 Authorization: <access token>
  │
[JwtTokenVerifier] 用 auth 的 /jwks 公钥验签（RS256）→ LoginUserDTO（userId/roleId/roleName/type）
  │  失败 → 401 JSON：40101 token 已过期 / 40102 无效 token / 40100 认证服务不可用
  ▼
[PrivilegeAuthorizationManager]
  ├─ 权限表中没有该"方法+路径" → 已登录即放行
  └─ 有 → 校验 roleId 是否在允许集合 → 不在则 403
  ▼
[UserInfoRelayFilter] 剥离客户端伪造的 user-info，注入 Base64(JSON(LoginUserDTO))
  ▼
路由转发（lb://xxx-service，StripPrefix=1）→ 业务服务
  ▼
[UserContextFilter] 解 user-info → SecurityContext（ROLE_*）+ UserContext → @PreAuthorize / gRPC 透传
```

### 2.3 刷新与登出

| 操作 | 路径 | 说明 |
|---|---|---|
| 前台刷新 | `GET /auth/accounts/refresh` | 读 Cookie `refresh`，校验签名 + `tokenType=refresh` + Redis JTI → 重新签发双 token |
| 管理端刷新 | `GET /auth/accounts/admin/refresh` | 同上，Cookie 名为 `admin-refresh` |
| 登出 | `POST /auth/accounts/logout` | 删除 Redis 中的 JTI + 清空 Cookie |

> `refresh token` 不能被当作 access token 使用：令牌里带 `tokenType` 声明，网关与 auth 服务都会校验。

---

## 3. Token 设计（RS256）

| 声明 | 含义 | 消费方 |
|---|---|---|
| `iss` | 签发者（`tj.auth.jwt.issuer`，默认 `tj-auth`） | 网关（`tj.auth.jwt.issuer` 与之比对，留空则不校验） |
| `exp` / `iat` | 过期时间 / 签发时间 | 验签方 |
| `userId` | 用户ID | 网关 → LoginUserDTO → 下游 |
| `roleId` | 角色ID（权限表按角色ID授权） | 网关鉴权 |
| `roleName` | 角色代号（admin/teacher/student） | 映射 `ROLE_ADMIN` 等 |
| `type` | 用户类型：1-员工 2-学员 3-老师 | 映射 `ROLE_STAFF/ROLE_STUDENT/ROLE_TEACHER` |
| `tokenType` | `access` / `refresh` | 防止 refresh token 当 access 用 |
| `jti` | refresh token 的唯一ID（存 Redis，登出即失效） | auth 服务 |

**角色口径统一**：`com.tjxt.tjcommon.Utils.RoleUtils` 是唯一映射入口（type/roleName → `ROLE_*`），
网关、auth 服务、SDK 三处共用，避免各写一份导致权限判断不一致。

---

## 4. 配置项

> Nacos 配置优先级高于各模块 `src/main/resources/application.yaml`（后者是本地可运行默认值）。

### 4.1 网关（`docker/nacos/gateway-service.yaml` + `tj-gateway/application.yaml`）

| 配置 | 说明 |
|---|---|
| `tj.auth.exclude-path` | 免认证白名单，支持 `/path` 与 `METHOD:/path` 两种 ant 风格写法 |
| `tj.auth.jwt.issuer` | 校验 token 的 issuer；留空则不校验（建议与 auth 保持一致后填写） |
| `tj.auth.jwt.public-key-path` | 可选：本地公钥 PEM（`classpath:` 或 `file:` / 裸路径），配置后不再访问 auth 服务，适合单机调试 |
| `tj.auth.jwks.service-name` | 公钥来源服务名（默认 `auth-service`，走服务发现） |
| `tj.auth.jwks.uri` | 可选：直连 `/jwks` 地址（如 `http://127.0.0.1:10011/jwks`） |
| `tj.auth.jwks.refresh-interval` | 公钥刷新间隔（默认 10m，支持 auth 服务密钥轮换） |
| `tj.auth.privilege.enabled` | 是否开启路径级鉴权（默认 true；置 false 则只认证不鉴权） |
| `tj.auth.privilege.refresh-interval` | 权限表刷新间隔（默认 20s，先比对版本号） |
| `tj.auth.privilege.timeout` | 读 Redis 超时（默认 3s，超时保留上一次快照） |

网关还需要 Redis 连接配置：`application.yaml` 已 import `optional:nacos:shared-redis.yaml`（`PrivilegeCache` 依赖）。

### 4.2 认证服务（`docker/nacos/auth-service.yaml` + `tj-auth/application.yaml`）

| 配置 | 说明 |
|---|---|
| `tj.auth.exclude-path` | auth 自身的安全白名单（登录、刷新、`/jwks`、文档、actuator） |
| `tj.auth.jwt.private-key-path` | PKCS#8 私钥（签发）。默认 `classpath:keys/private_key.pem` |
| `tj.auth.jwt.public-key-path` | X.509 公钥（`/jwks` 暴露、验签 refresh token） |
| `tj.auth.jwt.issuer` | 签发者，需与网关一致 |
| `tj.auth.jwt.token-ttl` / `refresh-ttl` / `remember-me-ttl` | 有效期（默认 30m / 7d / 7d） |

> 签名算法固定 RS256（`java-jwt` 的 `Algorithm.RSA256`），不通过配置切换。

### 4.3 业务服务（SDK）

| 配置 | 说明 |
|---|---|
| `tj.auth.exclude-path` | 业务服务自己的额外白名单（SDK 已内置 `/error/**`、`/actuator/**`、`/v3/api-docs/**`、`/favicon.ico`） |

---

## 5. 白名单与路由前缀（容易踩坑）

本项目路由带前缀 + `StripPrefix=1`（见 `docker/nacos/gateway-service.yaml`），因此
**白名单里要写"经过网关的原始路径"，不是后端接口路径**：

| 后端接口（auth-service） | 网关路径（白名单要写这个） |
|---|---|
| `/accounts/login` | `/auth/accounts/login` |
| `/accounts/admin/login` | `/auth/accounts/admin/login` |
| `/accounts/refresh` | `/auth/accounts/refresh` |
| `/accounts/admin/refresh` | `/auth/accounts/admin/refresh` |
| `/accounts/logout` | `/auth/accounts/logout` |
| `/jwks` | `/auth/jwks` |

新增一个"免认证接口"的步骤：

1. 在 Nacos `gateway-service.yaml` 的 `tj.auth.exclude-path` 里加上**网关路径**（可带方法前缀，如 `POST:/course/courses/search`）；
2. 如果是 auth 服务自己的公开接口，同时加到 `auth-service.yaml` 的 `tj.auth.exclude-path`（**后端路径**，不带 `/auth` 前缀）；
3. 白名单只影响"是否需要登录"，不影响权限表：命中白名单的请求不会再做路径级鉴权。

---

## 6. 路径级鉴权（权限表）

**数据协议**（auth 服务写、网关读，常量见 `com.tjxt.tjcommon.Constants.AuthConstants`）：

| Redis | 内容 |
|---|---|
| Hash `auth:privileges` | field = `METHOD:/uri`（如 `GET:/users/{id}`），value = `PrivilegeRoleDTO` 的 JSON（含 `roles`：允许的角色ID集合、`internal`） |
| String `version` | 权限版本号，每次权限变更 +1；网关据此判断是否需要重新拉取 |

**维护方式**：`tj-auth` 的 `PrivilegeController`/`PrivilegeServiceImpl`（新增/删除权限、绑定角色-权限）→ 每次变更整体覆盖 Hash 并 `version+1`。

**网关匹配规则**（`PrivilegeRule` + `PrivilegeRuleMatcher`）：

1. 先按方法匹配（权限带方法时必须一致，不带方法则不限方法）；
2. 再按 ant 路径匹配，候选路径有**两个**：网关原始路径与去掉第一段前缀的路径
   （`/user/users/1` 与 `/users/1` 都试，因此权限表里登记网关路径或后端路径都能命中）；
3. 命中规则后校验 `roleId`：`roles` 为空表示"登录即可访问"，否则必须在集合内；
4. 没有任何规则命中 → 只要求登录（与原项目"未配置权限即放行"一致）；
5. 多条命中时取先遍历到的一条（与老项目 `AuthUtil#findMatchPath` 行为一致）。

**降级行为**：Redis 不可用/超时 → 保留上一次快照并打 `WARN`；首次就不可用则快照为空 → 只认证不鉴权（不阻断业务，但也没有角色校验）。可用 `tj.auth.privilege.enabled=false` 显式关闭。

**与 `@PreAuthorize` 的分工**：网关管"这个角色能不能进这个接口"，业务服务管"这条数据这个人能不能动"。

---

## 7. 下游服务集成

- 网关注入的头：`user-info = Base64(JSON(LoginUserDTO))`（客户端自带的同名头会被**无条件剥离**，防止伪造）。
- SDK 自动配置（`tj-microservice-sdk` 的 `AutoConfiguration.imports`）：

| 自动配置 | 作用 |
|---|---|
| `UserContextAutoConfiguration` | 注册 `UserContextFilter` + 默认 `SecurityFilterChain`（无状态、白名单、401/403 JSON） |
| `GrpcClientAutoConfiguration` | 按"对应 Stub 是否存在"注册 8 个 `*GrpcClient`（缺通道不会启动失败） |
| `GrpcRelayAutoConfiguration` | 注册 gRPC 用户信息透传拦截器（客户端 + 服务端） |
| `RoleCacheConfig` / `CategoryCacheConfig` | 注册 Caffeine 缓存与 `RoleCache`/`CategoryCache`（依赖对应 gRPC 客户端） |

- 业务代码里取当前用户：

```java
// 方式一：Spring Security 上下文（推荐，支持 @PreAuthorize）
@PreAuthorize("hasRole('TEACHER')")
public void saveCourse() { ... }

// 方式二：SDK 的 UserContext（仅 gRPC 透传场景需要）
LoginUserDTO user = UserContext.get();
Long userId = UserContext.getUserId();
```

---

## 8. 排查手册

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 所有请求 401 `认证服务暂不可用` | 网关还没拿到 JWKS 公钥（auth 未启动/服务发现未就绪） | 看 `AuthFetchJwkThread` 日志；或配置 `tj.auth.jwt.public-key-path` 用本地公钥 |
| 401 `无效的 token`（token 明明是刚登录的） | issuer 不一致；网关公钥与 auth 私钥不配对；把 refresh token 当 access 用了 | 对齐 `tj.auth.jwt.issuer`；确认两边用同一对密钥；检查是否误传 refresh token |
| 401 `token 已过期` | access token 过期 | 调 `/auth/accounts/refresh` 重新获取 |
| 登录接口也被拦（401） | 白名单路径写成了后端路径（少了 `/auth` 前缀） | 见第 5 节对照表 |
| 403 `无访问权限` | 权限表命中了该路径但当前角色不在允许集合 | 在 auth 服务后台给角色绑定该权限，或调整 `roles` |
| 403 出现在以前能访问的接口 | 权限表里新增/改动了该路径的规则 | 看网关日志"网关鉴权拒绝：... 规则=... 当前角色=..." |
| 角色权限为空（`@PreAuthorize` 总是不通过） | token 里没有 `type` 声明（旧 token） | 重新登录获取新 token |
| 权限改动后网关不生效 | 版本号未变（权限写库但没刷新缓存）或超过 `refresh-interval` | 确认 `PrivilegeServiceImpl` 走了 `refreshPrivilegeCache()`；等待一个刷新周期（默认 20s） |
| 下游拿不到用户信息 | 请求没经网关（直连服务端口）或 `user-info` 被中间件丢弃 | 只通过网关访问；确认 `UserInfoRelayFilter` 顺序在安全链之后 |

---

## 9. 验证方式

单元测试（不依赖 Nacos/Redis/MySQL）：

```bash
mvn -pl tj-common,tj-microservice-sdk,tj-gateway,tj-auth test
```

| 测试类 | 覆盖内容 |
|---|---|
| `tj-auth` `JwtTokenFlowTest` | 签发→公钥验签往返、`type` 声明、过期=40101、refresh 不可当 access、refresh 流程 |
| `tj-gateway` `JwtTokenVerifierTest` | 合法/过期/篡改/issuer 不符/公钥未就绪/refresh 误用/角色映射 |
| `tj-gateway` `ExcludePathMatcherTest` | 白名单两种写法、方法限定、空白配置 |
| `tj-gateway` `PrivilegeRuleMatcherTest` | 权限表 JSON 解析、方法+路径匹配、路由前缀双候选、未配置权限即放行 |

手工联调（顺序：Nacos → Redis/MySQL → auth 10011 → gateway 10010）：

```bash
# 1. 登录拿 token
curl -X POST http://localhost:10010/auth/accounts/login \
     -H 'Content-Type: application/json' \
     -d '{"cellPhone":"13800000000","password":"123456"}'

# 2. 带 token 访问受保护接口
curl http://localhost:10010/user/users/1 -H "authorization: <access token>"

# 3. 不带 token（应 401 JSON）
curl -i http://localhost:10010/user/users/1

# 4. 公开接口（应 200）
curl http://localhost:10010/auth/jwks
```

---

## 10. 已知限制 / 后续可做

1. 权限表按"角色ID"授权，还没有"角色代号"或"用户级"规则；`internal` 字段目前不参与网关校验（与 auth 的 `AuthUtils` 行为一致）。
2. 网关鉴权依赖 Redis 可用；如需强一致，可改为启动时阻塞等待权限加载完成（当前是"启动不阻塞 + 定期刷新 + 失败保留快照"）。
3. `tj.auth.jwt.algorithm` 这类"看起来可配"的配置项已移除，算法固定 RS256；如需 RS512，需同时改 `JwtIssuer` 与网关 `JwtTokenVerifier`。
4. 下游若直连服务端口（绕过网关），`user-info` 缺失即视为未登录——请把业务端口限制在内网。
