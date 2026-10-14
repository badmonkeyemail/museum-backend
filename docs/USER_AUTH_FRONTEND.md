# 用户认证与前端闲置锁屏接入

本仓库目前是 Spring Boot 后端仓库，没有 Vue 前端源码。下面是 Vue 3 前端必须实现的调用约定。

## 登录

`POST /api/auth/login`

```json
{ "username": "super", "password": "从部署环境配置的初始密码" }
```

成功响应包含 `token`、`user.roles`、`idleTimeoutSeconds`、`heartbeatIntervalSeconds`。把 token 放入 `sessionStorage`（不要放入 localStorage），每个受保护的请求携带：

```http
Authorization: Bearer <token>
```

## 闲置锁屏行为

- 前端在鼠标、键盘、触摸、滚动等用户活动时重置闲置计时器。
- 超过 `idleTimeoutSeconds` 后显示锁屏遮罩，并**停止心跳**。
- 未锁屏时按 `heartbeatIntervalSeconds` 调用 `POST /api/auth/heartbeat`。
- 解锁时调用 `POST /api/auth/unlock`，请求体为 `{"password":"当前用户密码"}`。
- 如果解锁返回 401，说明服务端租约已过期或会话失效，清除 token 并返回登录页。
- 用户主动退出调用 `POST /api/auth/logout`，成功后清除 token。
- 后端不依赖浏览器发送 `unload` 事件释放会话；页面崩溃/电脑断电时，停止心跳后租约最多等待配置的 `session-lease-seconds` 才过期。

## Vue 3 composable 示例（`src/composables/useIdleLock.ts`）

```typescript
import { onMounted, onBeforeUnmount, ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export function useIdleLock(
  token: () => string | null,
  onSessionExpired: () => void,
  idleTimeoutSeconds = 900,
  heartbeatIntervalSeconds = 30,
) {
  const locked = ref(false)
  const idleTimeoutMs = idleTimeoutSeconds * 1000
  const heartbeatMs = heartbeatIntervalSeconds * 1000
  let idleTimer: ReturnType<typeof setTimeout> | undefined
  let heartbeatTimer: ReturnType<typeof setInterval> | undefined

  async function post(path: string, body?: unknown) {
    const currentToken = token()
    const response = await fetch(API_BASE + path, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(currentToken ? { Authorization: `Bearer ${currentToken}` } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    return response.status === 204 ? undefined : response.json()
  }

  function stopHeartbeat() {
    if (heartbeatTimer) clearInterval(heartbeatTimer)
    heartbeatTimer = undefined
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(async () => {
      if (locked.value || !token()) return
      try {
        await post('/api/auth/heartbeat')
      } catch {
        stopHeartbeat()
        onSessionExpired()
      }
    }, heartbeatMs)
  }

  function lock() {
    locked.value = true
    stopHeartbeat()
    if (idleTimer) clearTimeout(idleTimer)
  }

  function resetIdleTimer() {
    if (locked.value || !token()) return
    if (idleTimer) clearTimeout(idleTimer)
    idleTimer = setTimeout(lock, idleTimeoutMs)
  }

  function onActivity() {
    resetIdleTimer()
  }

  async function unlock(password: string) {
    try {
      await post('/api/auth/unlock', { password })
      locked.value = false
      resetIdleTimer()
      startHeartbeat()
      return true
    } catch {
      onSessionExpired()
      return false
    }
  }

  async function logout() {
    stopHeartbeat()
    if (idleTimer) clearTimeout(idleTimer)
    try { await post('/api/auth/logout') } catch { /* lease will expire automatically */ }
    onSessionExpired()
  }

  onMounted(() => {
    for (const eventName of ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'pointerdown']) {
      window.addEventListener(eventName, onActivity, { passive: true })
    }
    resetIdleTimer()
    startHeartbeat()
  })

  onBeforeUnmount(() => {
    for (const eventName of ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'pointerdown']) {
      window.removeEventListener(eventName, onActivity)
    }
    if (idleTimer) clearTimeout(idleTimer)
    stopHeartbeat()
  })

  return { locked, unlock, logout, resetIdleTimer }
}
```

创建 composable 时，将登录响应中的 `idleTimeoutSeconds` 和 `heartbeatIntervalSeconds` 作为第三、第四个参数传入。前端必须在锁屏时暂停心跳；否则服务端无法区分“用户仍在操作”和“页面已闲置锁定”。
