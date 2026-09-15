# 开发环境内置 RSA 密钥对（RS256）

- `private_key.pem`：PKCS#8 私钥，auth 服务签发 token 用（`tj.auth.jwt.private-key-path`）
- `public_key.pem`：X.509 公钥，auth 服务 `/jwks` 暴露、网关与下游服务验签用（`tj.auth.jwt.public-key-path`）

文件命名与 `docker/nacos/auth-service.yaml` 中配置的 `classpath:keys/private_key.pem` /
`classpath:keys/public_key.pem` 保持一致，本地直接启动即可签发与验签。

> 这两个文件仅用于本地开发/联调，**生产环境必须替换**：部署时按 auth-service.yaml 注释中的方式，
> 用 Docker secret/挂载把 `/app/keys/private_key.pem` 与 `/app/keys/public_key.pem` 提供给容器，
> 并把私钥从代码仓库中移除。网关不需要私钥，它通过服务发现拉取 auth 的 `/jwks` 公钥
> （也可配置 `tj.auth.jwt.public-key-path` 使用本地公钥离线验签）。

生成方式（任选其一）：

```bash
# openssl
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl rsa -in private_key.pem -pubout -out public_key.pem
```

密钥轮换：替换 auth 服务的密钥文件并重启，网关会按 `tj.auth.jwks.refresh-interval`（默认 10 分钟）
自动重新拉取公钥；验签失败时也会立即触发一次带节流的刷新。
