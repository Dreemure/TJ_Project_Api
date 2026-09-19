# 内网链路 TLS / mTLS 改造清单（不动业务代码、不动前端、不动鉴权模型）

> 目的：把"数据传输安全"补齐到 HTTPS 真正覆盖不到的内网那几跳。
> 适用前提：**前端请求方式不变**（仍然只调网关同一个域名、只带 `Authorization` 头），
> **鉴权模型不变**（网关验 JWT → 注入 `user-info` → SDK 解 `user-info` → `@PreAuthorize`）。
> 本改造只换"传输层"，不引入请求签名、不改任何接口契约、不要求前端改代码。

---

## 1. 为什么需要这一步

HTTPS 的保护范围**只到 TLS 终止点为止**。本项目外部 HTTPS 终止在 Nginx Proxy Manager，
终止点之后的链路目前都是明文：

| 内网跳 | 现状 | 风险 | 处理 |
|---|---|---|---|
| 网关 → auth 的 `/jwks`（取验签公钥） | 曾是明文 `http://host:port/jwks` | 中间人替换"验签公钥"→ 可伪造任意 token（比不验签更危险） | **已修**：网关默认改用本地公钥；明文拉取默认拒绝 |
| 网关 → 各业务服务的 HTTP 路由（`lb://`） | 明文 | 内网可窃听/改包；Token 也会被内网看到 | 本清单第 2/3 步 |
| 服务 ↔ 服务的 gRPC（`HTTP 端口 + 1000`） | 明文，且身份头 `user-info` 是**明文 JSON 且服务端零校验** | 谁能连上 gRPC 端口就能冒充任意用户 | 本清单第 3 步（mTLS 是这条的前提） |
| 外部 → 网关 | HTTPS（NPM 终止） | — | 保持；建议补 HSTS + http→https 跳转 |

结论：**"只靠 HTTPS"要成立，前提是 HTTPS 铺满每一跳**；否则内网任意一个被突破的容器/进程
都能改包、甚至直接伪造身份。这里不做请求签名（本项目暂不需要），改用传输层方案。

---

## 2. 第一步：先做零风险的收敛（无业务改动）

1. **验签公钥钉在网关本地** —— 已完成：`tj.auth.jwt.public-key-path: classpath:keys/public_key.pem`
   （生产建议与 auth 共用挂载文件：`file:/app/keys/public_key.pem`）。
2. **服务端口不要发布到宿主机**：`server.port` / gRPC 端口只在容器/内网可达，
   外部只能经网关进入（`docker-compose.yaml` 里暴露的端口逐个过一遍，业务服务不 publish）。
3. **Nacos 必须开认证**（已开：`NACOS_AUTH_ENABLE=true`），并更换默认口令（`nacos/nacos` 仍是默认值）。
4. **数据库/缓存/中间件口令**：`docker-compose.yaml` 里 MySQL `root/root`、Redis 无密码等
   都是默认值，生产必须换成随机强口令 + 不发布端口。

---

## 3. 第二步：内部单向 TLS（先加密，不做双向认证）

### 3.1 生成内部 CA 与各服务证书（一次性）

```bash
# 1) 内部 CA（私钥只留在部署机 / 密钥管理系统，不进仓库）
openssl req -x509 -newkey rsa:3072 -sha256 -days 3650 -nodes \
  -keyout internal-ca.key -out internal-ca.crt \
  -subj "/CN=tjxt-internal-ca"

# 2) 每个服务一张证书，SAN 必须包含它的 DNS 名（= Nacos 服务名，服务间按名字互访）
gen_svc_cert() {
  SVC=$1
  openssl req -newkey rsa:2048 -nodes -keyout ${SVC}.key -out ${SVC}.csr \
    -subj "/CN=${SVC}"
  printf "subjectAltName=DNS:%s,DNS:%s.tj-spring,DNS:localhost,IP:127.0.0.1\n" "$SVC" "$SVC" > ${SVC}.ext
  openssl x509 -req -in ${SVC}.csr -CA internal-ca.crt -CAkey internal-ca.key \
    -CAcreateserial -days 825 -sha256 -extfile ${SVC}.ext -out ${SVC}.crt
}
for s in auth-service user-service course-service trade-service gateway-service; do gen_svc_cert $s; done
```

### 3.2 每个服务装载证书（SSL Bundle，PEM 形式）

```yaml
spring:
  ssl:
    bundle:
      pem:
        internal:                                   # bundle 名字，下面各协议引用它
          keystore:
            certificate: file:/app/keys/${spring.application.name}.crt
            private-key: file:/app/keys/${spring.application.name}.key
            # private-key-password: ${TLS_KEY_PASSWORD}   # 私钥加密时填
          truststore:
            certificate: file:/app/keys/internal-ca.crt  # 内部 CA，用于校验对端/自身链
```

### 3.3 让 HTTP 与 gRPC 都用上这个 bundle

```yaml
server:                       # 服务自身 HTTP 端口（网关路由 /auth/** 等会走这里）
  ssl:
    enabled: true
    bundle: internal

spring:
  grpc:
    server:                   # gRPC 服务端（默认端口 = HTTP 端口 + 1000）
      ssl:
        enabled: true
        bundle: internal
        client-auth: none      # 第一步先单向 TLS；第三步改 optional/require
    client:
      channel:
        user-service:          # 通道名 = spring.grpc.client.channel.<服务名>.target 里用的那个名字
          ssl:
            enabled: true
            bundle: internal
```

### 3.4 网关路由到服务改成 https

Spring Cloud Gateway 通过 `lb://` 走的是 http；走 https 需要把路由 uri 换成 https 目标
（`lbs://服务名` 的写法在本项目 Spring Cloud 2025.1.x 上**请先本地验证一次**，
或改用 `uri: https://服务名:端口` 直连 + 负载均衡器）：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: auth
              uri: lbs://auth-service      # ← 待验证：https 版的负载均衡前缀
              predicates: [ Path=/auth/** ]
```

> 落地顺序建议：先给 **gRPC** 开 TLS（这条链路的身份伪造风险最高），再给 HTTP 路由开；
> 每步只改一个服务的配置、观察行后再推广，任一步异常都能单点回滚。

---

## 4. 第三步：升级为 mTLS（服务身份互认）

把 `client-auth` 打开，服务端要求客户端出示证书；客户端侧同样带上 keystore/truststore：

```yaml
spring:
  grpc:
    server:
      ssl:
        client-auth: require      # none → optional（观察期）→ require（强制）
server:
  ssl:
    client-auth: need             # servlet 端同理
```

要点：

- **先 `optional` 观察**：既能收证书、也放行没证书的调用，看日志确认全部服务都已带证书，再切 `require`。
- 证书里的 `CN`/SAN 就是**服务身份**，比共享密钥强：不需要在配置里散落服务间口令。
- 轮换：证书有效期（示例 825 天）与内部 CA 的签发流程要纳入运维日历；建议到期前自动续签并支持双证书过渡。
- **gRPC 的 `user-info` 头只有在 mTLS 下才谈得上可信** —— 单向 TLS 只保证"连接加密"，
  不保证"对面是谁"；只有 mTLS 才能确认"这条 gRPC 调用确实来自受信服务"，
  否则任何能连上端口的进程都能自称任意用户。

---

## 5. 本清单已核对的配置键（本机 Spring Boot / spring-grpc 4.1.1 jar 反编译核对）

| 用途 | 配置键 |
|---|---|
| 证书/密钥装载（PEM） | `spring.ssl.bundle.pem.<bundle>.keystore.certificate`、`.keystore.private-key`、`.keystore.private-key-password`、`.truststore.certificate` |
| servlet 服务端 TLS | `server.ssl.enabled`、`server.ssl.bundle`、`server.ssl.client-auth`（另有 `server.ssl.certificate` / `trust-certificate` 等直接指定文件的写法） |
| gRPC 服务端 TLS | `spring.grpc.server.ssl.enabled`、`.bundle`、`.client-auth`（`NONE/OPTIONAL/REQUIRE`）、`.secure` |
| gRPC 客户端 TLS | `spring.grpc.client.channel.<通道名>.ssl.enabled`、`.bundle` |

待你本地确认的一项：网关 `lb://` → https 的写法（`lbs://`）是否在当前 Spring Cloud 版本下生效。

---

## 6. 明确不做 / 仍需知道的残留风险

- **不引入请求级签名**：因此**不防重放**（合法签名/合法请求可被重放）、不提供"不可抵赖"的审计证据。
  真需要这两点时，才值得付出规范串、nonce 存储、密钥轮换的成本（跨公司接口、支付回调等场景）。
- **不防"被入侵的合法对端"**：mTLS 只证明"是谁"，不证明"它此刻想干的是对的"；
  业务侧仍然必须以服务端为准做校验（金额、归属、状态），并保留网关的路径级鉴权。
- **前端用户改自己的请求**：传输层与签名都解决不了，只能靠服务端权威校验 + 越权检查。
