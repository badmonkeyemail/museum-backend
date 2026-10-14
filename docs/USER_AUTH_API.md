# 用户认证 API 与部署说明

## 1. 首次启动

在部署环境设置：

- `MUSEUM_SUPER_USERNAME`：内置超级用户名称，默认 `super`。
- `MUSEUM_SUPER_PASSWORD`：**首次启动必须设置**，至少 12 位。系统只在数据库尚不存在 SUPER 角色时初始化用户，不会在后续启动时重置密码。
- `MUSEUM_IDLE_TIMEOUT_SECONDS`：前端闲置锁屏秒数，默认 900 秒。
- `MUSEUM_SESSION_LEASE_SECONDS`：服务端会话租约，默认 120 秒，最小 45 秒。
- `MUSEUM_HEARTBEAT_INTERVAL_SECONDS`：前端心跳周期，默认 30 秒。服务端会将其限制为不超过租约的一半。

首次启动会运行 Flyway 的 `V2__user_authentication.sql`，创建用户、角色和单活跃会话表。部署前请先备份数据库，并在测试数据库验证 Flyway 升级。

## 2. API

所有受保护接口都需要 `Authorization: Bearer <token>`。

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 公开 | 登录；系统已被占用时返回 409 |
| POST | `/api/auth/logout` | 登录用户 | 主动释放全局会话 |
| POST | `/api/auth/heartbeat` | 登录用户 | 刷新会话租约 |
| POST | `/api/auth/unlock` | 登录用户 | 锁屏后使用当前用户密码解锁 |
| POST | `/api/auth/change-password` | 登录用户 | 修改自己的密码 |
| GET | `/api/auth/me` | 登录用户 | 获取当前用户和权限 |
| GET | `/api/users` | SUPER | 查询用户 |
| POST | `/api/users` | SUPER | 创建 ADMIN 用户 |
| PATCH | `/api/users/{id}/enabled` | SUPER | 启用/停用用户 |
| PATCH | `/api/users/{id}/admin-role` | SUPER | 给用户添加/移除 ADMIN 权限 |
| PUT | `/api/users/{id}/password` | SUPER | 重置用户密码 |

### 登录示例

```http
POST /api/auth/login
Content-Type: application/json

{"username":"super","password":"<部署时设置的密码>"}
```

如果当前租约尚未过期，其他用户登录返回 HTTP 409；用户名或密码错误返回 HTTP 401。

### 创建 ADMIN 用户

```http
POST /api/users
Authorization: Bearer <super-token>
Content-Type: application/json

{"username":"museum-admin","initialPassword":"至少12位的初始密码"}
```

创建接口只授予 `ADMIN`，不允许通过普通用户管理 API 创建额外的 `SUPER`。超级用户可通过 `PATCH /api/users/{superId}/admin-role` 为自己增加 `ADMIN` 权限，使同一用户同时拥有两种权限。

## 3. 单用户会话与异常退出

数据库的 `app_active_session` 固定只有 `id=1` 一行。登录时通过事务锁定该行，串行化多个并发登录；只保存随机 token 的 SHA-256 摘要，不保存明文 token。所有后端实例共享该数据库，因此部署多个应用实例时也执行同一单用户规则。

活跃会话依靠前端心跳续租。浏览器崩溃、断电或网络中断后，心跳停止，租约过期后允许其他用户登录。正常退出会立即清空租约。若要更快回收异常会话，可以缩短租约，但心跳周期必须明显短于租约，且应考虑网络抖动。

## 4. 权限模型

- `SUPER`：管理用户；不能通过用户管理接口修改其他超级用户。
- `ADMIN`：执行业务模块操作。
- 同时具备 `SUPER` 与 `ADMIN` 的用户既能管理用户，也能操作业务模块。
- 没有 `ADMIN` 的纯 `SUPER` 用户不能调用 `/api/**` 下的业务模块接口。

## 5. 上线前检查

1. 配置强密码和数据库访问凭据，不要把密钥提交到 Git。
2. 验证迁移能在现有数据库的备份副本上成功运行。
3. 验证 ADMIN 无法访问 `/api/users/**`，未登录请求无法访问业务接口。
4. 验证两个浏览器同时登录时第二个得到 409。
5. 验证主动退出立即允许其他用户登录；停止心跳后等待租约过期也可重新登录。
6. 验证锁屏会停止心跳，正确密码可以在租约未过期时解锁；租约过期后应返回登录页。
7. 当前仓库没有 Vue 前端源码，闲置锁屏 composable 的接入说明见 `USER_AUTH_FRONTEND.md`。
