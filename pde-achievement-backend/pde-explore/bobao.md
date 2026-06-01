## 全员动态播报（SSE）

### GET /ulivepde/api/activity/stream

> ⏳ **待实现**
>
> **前端使用位置**：地图页（`Index.jsx`）右侧面板下半区域「⚡ 全员动态」列表，实时展示所有用户的通关/挑战动态。

SSE（Server-Sent Events）长连接接口。服务器主动推送，客户端保持一条持久连接，有新动态时立即收到，无动态时零开销。

**为什么用 SSE 而不是轮询**：系统预计上万人使用。轮询每15秒一次 = 每分钟 40 万次请求；SSE 每人一条长连接，服务器只在有事件时才发数据，空闲时几乎零开销。

---

**Response Headers**

```
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
X-Accel-Buffering: no
```

> `X-Accel-Buffering: no` 是关键，告诉 Nginx 不要缓冲响应，否则消息会积压延迟。

---

**推送格式**

每条动态一个 SSE event，`data:` 后跟 JSON，以两个换行结束：

```
data: {"id":1234567890,"name":"王小明","avatar":"王","type":"complete","stageId":3,"stageTitle":"AI战伴绑定","rankName":"🥇 黄金","time":"刚刚"}

data: {"id":1234567891,"name":"李思远","avatar":"李","type":"start","stageId":4,"stageTitle":"本地代码跑起来","rankName":"💎 铂金","time":"刚刚"}
```

**字段说明**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | long | 事件唯一 ID，用时间戳即可 |
| name | string | 用户姓名 |
| avatar | string | 姓名首字，前端用于显示头像圆 |
| type | string | `complete`（完成关卡）或 `start`（开始关卡） |
| stageId | int | 关卡编号 1-7 |
| stageTitle | string | 关卡标题，从 stage 表取 |
| rankName | string | 段位文字，见下方映射 |
| time | string | 固定传 `"刚刚"`，前端展示用 |

**段位映射（stageId → rankName）**

| stageId | rankName |
|---------|----------|
| 1 | 🥉 青铜 |
| 2 | 🥈 白银 |
| 3 | 🥇 黄金 |
| 4 | 💎 铂金 |
| 5 | 💠 钻石 |
| 6 | ⭐ 星耀 |
| 7 | 🏆 最强王者 |

---

**实现方案**

核心是一个进程内的**连接池广播**，不需要引入消息队列：

```
// 1. 全局维护一个连接池
connections = Set<HttpServletResponse>

// 2. SSE 接口：将连接加入池，挂起不关闭
GET /ulivepde/api/activity/stream:
    设置 headers
    connections.add(response)
    客户端断开时: connections.remove(response)
    每 30 秒发一次心跳: ": ping\n\n"（防 Nginx 超时断连）
    挂起，不 return

// 3. 广播方法
void broadcast(ActivityEvent event):
    String msg = "data: " + JSON.stringify(event) + "\n\n"
    for conn in connections:
        try: conn.getWriter().write(msg); conn.getWriter().flush()
        catch: connections.remove(conn)  // 连接已断，清理

// 4. 在现有完关逻辑里新增一行调用
POST /ulivepde/api/progress/step（stageCompleted=true 时）:
    ... 原有写库逻辑不变 ...
    broadcast(new ActivityEvent(user, "complete", stageId, ...))

POST /ulivepde/api/progress/verify（verifyStatus=passed 时）:
    ... 原有写库逻辑不变 ...
    broadcast(new ActivityEvent(user, "complete", stageId, ...))
```

**连接建立时推送最近10条历史**，让用户打开页面就能看到内容：

```
连接建立后，立即查询最近10条通关记录（ORDER BY passed_at DESC LIMIT 10）
逐条写入 SSE，然后挂起等待新事件
```

---

**Nginx 配置（必须）**

```nginx
location /ulivepde/api/activity/stream {
    proxy_pass http://backend;
    proxy_buffering off;        # 关闭缓冲，消息实时到达
    proxy_cache off;
    proxy_read_timeout 3600s;   # 保持连接1小时
    proxy_set_header Connection '';
    proxy_http_version 1.1;
}
```

---

**连接数估算**

| 场景 | 同时在线 | 说明 |
|------|---------|------|
| 日常 | ~50人 | 可忽略 |
| 高峰（全员培训） | ~500人 | 正常 |
| 极限 | 1万人 | 约 200MB 内存，注意调大 Tomcat `maxConnections`（默认 8192 够用） |

---

**多实例部署注意**

如果后端多节点部署，`broadcast()` 只能通知当前节点的连接。跨节点方案：用 **Redis Pub/Sub**，每个节点订阅同一个 channel，收到消息后广播给本节点的连接池。

---

**鉴权**

前端已带 `withCredentials: true`，SSO Cookie 自动附带，后端按现有 Cookie 校验逻辑处理即可，无需额外适配。

---

## 关卡解锁规则

1. 第 1 关对所有用户默认开放，无记录时 `currentStage=1`
2. 第 N 关通关后，第 N+1 关自动解锁（实时计算，不存储）
3. `verify_type=self`：步骤全部勾选后由 `POST /api/progress/step` 自动写入通关记录
4. 其他类型：运营后台审核通过后更新 `verify_status='passed'` 并写入 `passed_at`
