# 业务接口请求签名（防篡改 / 防重放）

> 面向**服务端↔服务端**与**合作方回调**接口。浏览器前端**不要用**这套（私钥在前端等于没有密钥）。
> 前端请求的防篡改靠服务端权威校验 + JWT（见 [`authentication.md`](authentication.md)）。

---

## 1. 先想清楚：什么时候该用签名，什么时候不必

| 场景 | 推荐方案 | 为什么 |
|---|---|---|
| 内网服务间互通（本项目自己的服务互调） | **mTLS**（见 [`internal-tls.md`](internal-tls.md)） | 传输层解决，不需要改业务代码、不需要规范串与 nonce 逻辑；签名与之叠加是重复投入 |
| 合作方/第三方调用你的接口、支付类回调 | **请求签名（本文）** | 双方没有共享网络信任，只需要"某把公钥签过的数据"，还能留不可抵赖的审计证据 |
| 你签名下发、客户端验证（防篡改响应） | 请求签名的反向用法（`ApiSignHelper` 同样可用） | 私钥留在服务端，公钥可公开下发 |
| 浏览器前端参数防篡改 | **不要用签名** | 用户自己持有私钥，可以对自己提交的任何内容重新签名；只能靠服务端校验（金额、归属、库存以服务端为准） |

一句话：**签名的价值在"跨信任域"和"不可抵赖"**；同一份内网里，mTLS 更省事。

---

## 2. 业务接口怎么开启（默认关闭，零影响）

签名校验由 `tj-common` 的 `ApiSignatureFilter` 完成，**默认不注册**；某个服务需要时在 Nacos 里开：

```yaml
tj:
  auth:
    sign:
      enabled: true                 # 开关（默认 false）
      include-path:                 # 只保护这些路径，其余请求完全不参与校验
        - /v2/partner/**
      exclude-path:                 # 可选：白名单（优先级高于 include-path）
        - /v2/partner/health
      tolerance: 5m                 # 时间戳容差（双向）
      nonce-ttl: 10m                # nonce 去重记录保留时间
      max-body-bytes: 1048576       # 请求体上限，超过直接拒绝（不做无上限缓冲）
      # strip-path-prefix: /course  # 可选：验签路径与签名方不一致时去掉前缀（例如经网关 StripPrefix）
      apps:
        partner-a:                  # appId
          public-key: MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...   # Base64(SPKI) 公钥
          remark: 某某合作方
        order-service:
          public-key-path: file:/app/keys/order-service.pub.pem  # 也可以给 PEM 文件
```

要点：

- **公钥只从配置读**，请求里带的公钥一概不用（这一点决定验签有没有意义）。
- `include-path` 为空 = 不校验任何请求（不会"看起来开了其实没生效"，启动日志会 WARN 提示）。
- nonce 去重优先用 **Redis**（多实例安全）；拿不到 Redis 时回退单机内存实现并打 WARN，**多实例部署下重放保护不完整**。
- 校验通过后会把 `appId` 放进请求属性 `ApiSignHelper.ATTR_APP_ID`，业务侧可据此做数据隔离。

### 校验顺序（fail-closed）

1. 路径是否命中 `include-path`（没命中直接放行，零开销）
2. 四个签名头是否齐全且格式合法
3. 时间戳是否在容差内（**双向**：过期与"未来时间"都拒绝，否则可以签一个长期有效的请求）
4. `appId` 是否已配置 → 取服务端固定公钥
5. 签名是否正确（**先验签，再消费 nonce**，否则攻击者能用无效签名刷掉合法 nonce）
6. nonce 是否首次出现（Redis `SETNX` + TTL）

任一步失败 → `401` + `R` 格式 JSON；**对外只给笼统信息，细节只写日志**，不给攻击者当探针。

---

## 3. 待签串（canonical）契约 —— 双方必须逐字节一致

固定 6 段，`\n` 连接，顺序固定、不排序、不做 URL 解码：

```
appId
method           大写（GET/POST/...）
pathAndQuery     验签方看到的"后端路径 + 原始 query"（不含域名、不含 context-path）
timestamp        Unix 秒
nonce            一次性随机串（8~64 位，[A-Za-z0-9_-]）
bodySha256Hex    请求体原始字节的 SHA-256 小写十六进制；无请求体时是 e3b0c442...b855
```

请求头（常量在 `AuthConstants`）：

| 头 | 含义 |
|---|---|
| `x-sign-app-id` | 调用方标识（对应配置里的 `apps.<appId>`） |
| `x-sign-timestamp` | Unix 秒 |
| `x-sign-nonce` | 一次性随机串 |
| `x-sign-signature` | Base64 的 IEEE P1363 签名（ECDSA P-256 + SHA-256，与前端 Web Crypto 输出一致） |
| `x-sign-body-sha256` | 可选，便于排查"双方 canonical 不一致"，服务端仍以实际收到的字节为准 |

**三条最容易踩的坑**（写错任一条 = 签名永远验不过，或验签形同虚设）：

1. `pathAndQuery` 必须是**验签方看到的路径**。本项目网关 `StripPrefix=1`，所以经网关的调用要签去掉前缀后的路径，
   或给验签方配 `tj.auth.sign.strip-path-prefix`。
2. `bodySha256Hex` 必须对**实际发送的字节**取摘要：JSON 不要重新序列化（键序、空格、数字格式一变就全不一样）。
3. 双方都调用 `ApiSignHelper`，不要各写各的拼接逻辑。

---

## 4. 调用方（签名方）怎么写

服务端↔服务端（Java）：

```java
long ts = Instant.now().getEpochSecond();
String nonce = RandomUtils.randomString(16);          // 每次请求必须不同
byte[] body = json.getBytes(StandardCharsets.UTF_8);  // 发出去的就是这些字节

String signature = ApiSignHelper.signRequest(
        "order-service", "POST", "/v2/partner/orders", ts, nonce, body, privateKey);

request.header("Authorization", token)
       .header(AuthConstants.SIGN_APP_ID_HEADER, "order-service")
       .header(AuthConstants.SIGN_TIMESTAMP_HEADER, String.valueOf(ts))
       .header(AuthConstants.SIGN_NONCE_HEADER, nonce)
       .header(AuthConstants.SIGN_SIGNATURE_HEADER, signature)
       .body(body);
```

合作方（Node / 浏览器外的服务端，Web Crypto 即可）：

```js
const enc = new TextEncoder();
const canonical = [appId, method.toUpperCase(), pathAndQuery, String(ts), nonce,
                   sha256Hex(bodyBytes)].join('\n');
const key = await crypto.subtle.importKey('pkcs8', privateKeyDer,
    { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign']);
const sig = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, key, enc.encode(canonical));
// sig 是 64 字节 r‖s，Base64 之后放进 x-sign-signature
```

> 前端浏览器里如果确实需要（例如合作方在自己的管理系统里调试），注意：**这对安全没有帮助**，
> 只是为了联调方便。真正的密钥不能下发到浏览器。

---

## 5. 服务端（验签方）怎么写

**什么都不用写**：按第 2 节开配置即可，过滤器会在进 Controller 之前拦掉非法请求。

业务代码里如果需要按调用方区分数据：

```java
Object appId = request.getAttribute(ApiSignHelper.ATTR_APP_ID);
```

如果只想对单个接口做校验、不想开全局过滤器，也可以直接调工具类：

```java
boolean ok = ApiSignHelper.verifyRequest(appId, "POST", "/v2/partner/orders",
        timestamp, nonce, bodyBytes, signatureBase64, pinnedPublicKeyBase64);
```

（记得自己再补上时间戳容差与 nonce 一次性校验，否则仍然可以被重放。）

---

## 6. 密钥与运维

- **每个调用方一把密钥**，公钥按 `appId` 配到服务端；密钥泄漏时只影响该 appId，便于单独吊销。
- 生成密钥（私钥留在调用方侧，绝不进仓库、绝不通过接口传输）：

  ```bash
  openssl ecparam -genkey -name prime256v1 -noout -out partner-a.key
  openssl pkcs8 -topk8 -nocrypt -in partner-a.key -out partner-a.pk8   # 导出 PKCS#8（给 Node 用）
  openssl ec -in partner-a.key -pubout -out partner-a.pub.pem          # 公钥（配置到服务端）
  ```

- 公钥也可直接给 Base64(SPKI)：`openssl ec -in partner-a.key -pubout -outform DER | base64 -w0`。
- **轮换**：调用方先生成新密钥并把新公钥给你 → 你同时配置新旧两把（`apps` 里加一个临时 appId，
  或在代码里支持 `public-key` + `public-key-previous`，需要的话我可以补）→ 调用方切换 → 观察无异常后移除旧公钥。
- `appId` 的 `public-key` 配错时，**所有该调用方的请求都会 401**，日志会明确写"调用方未配置公钥/公钥无法解析"，便于定位。

---

## 7. 这套机制不解决的问题（别指望它）

- **不解决浏览器用户改自己的请求**（私钥在前端 = 用户自己会签）。
- **不解决身份之外的授权**：验签只证明"是 partner-a 发的"，不证明"partner-a 有权做这件事"，
  仍要按业务做权限与数据隔离。
- **不解决被入侵的合法调用方**：对方的私钥在它手里，它想签什么都行（mTLS 同理）。
- **不替代传输加密**：签名保证完整性/来源，不保证机密性；敏感数据仍要走 HTTPS。
- **性能**：ECDSA 验签实测约 **0.6ms/次（单核，本机）**，所以只对必要路径开启（`include-path`），
  不要全站开启；内网高频互调优先考虑 mTLS。

---

## 8. 相关代码位置

| 文件 | 职责 |
|---|---|
| `tj-common/.../Utils/EcdsaHelper.java` | ECDSA P-256 签名/验签（严格 64 字节、失败可观测、fail-closed） |
| `tj-common/.../Utils/ApiSignHelper.java` | **待签串契约** + 摘要 + 时间戳/nonce 校验 + 签名/验签一步到位 |
| `tj-common/.../Sign/ApiSignatureFilter.java` | 业务接口的验签过滤器（含请求体缓存，下游仍可读 body） |
| `tj-common/.../Sign/ApiSignProperties.java` | `tj.auth.sign.*` 配置 |
| `tj-common/.../Sign/{NonceStore,RedisNonceStore,InMemoryNonceStore}.java` | nonce 去重（防重放） |
| `tj-common/.../Constants/AuthConstants.java` | 签名请求头常量 |
| `tj-common/src/test/java/.../Sign/ApiSignatureFilterTest.java` | 端到端测试：正常/篡改/过期/重放/未授权/超限体 |
