# 终端命令校验接口说明

> 分支：`feature/yanligame`
> 负责人：yanli06
> 用途：第1-3关通关校验，用户在终端运行命令后自动上报，再点按钮完成通关

---

## 背景

第1-3关（curl 类型）的通关流程：
1. 用户复制页面上的命令
2. 在终端运行（命令里含 curl，自动上报到后端）
3. 点"我跑完了，提交校验"按钮
4. 后端查有无上报记录 → 通关

---

## 新增文件

| 文件 | 说明 |
|------|------|
| `src/main/java/com/meituan/pde/controller/VerifyController.java` | 新增 token 和 checkin 接口 |
| `src/main/java/com/meituan/pde/dao/VerifyCheckinLogDao.java` | DAO 接口 |
| `src/main/java/com/meituan/pde/entity/VerifyCheckinLog.java` | 实体类 |
| `src/main/resources/mapper/VerifyCheckinLogDao.xml` | MyBatis mapper |

## 修改文件

| 文件 | 改动 |
|------|------|
| `src/main/java/com/meituan/pde/controller/ProgressController.java` | `value` 改为非必填（curl 类型不需要传） |
| `src/main/java/com/meituan/pde/service/ProgressService.java` | 新增 `curl` 类型处理逻辑 |
| `src/main/resources/sql/schema.sql` | 新增 `verify_checkin_log` 表 |

---

## 需要同事做的事

### 1. 线上数据库建表

在 **MySQL**（`ulivepde_ulivepdeweb_test`）执行以下 DDL：

```sql
CREATE TABLE IF NOT EXISTS verify_checkin_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_mis VARCHAR(64) NOT NULL,
    stage_id INT NOT NULL,
    version_info VARCHAR(512) NOT NULL DEFAULT '',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_verify_checkin_mis_stage (user_mis, stage_id, created_at)
);
```

> 注意：`schema.sql` 里是 SQLite 语法（本地用），线上 MySQL 建表用上面这个。

### 2. 部署到测试环境

把 `feature/yanligame` 分支部署到测试环境即可，代码已就绪。

---

## 接口说明

### GET `/ulivepde/api/verify/token`

前端加载关卡页时调用，获取当前用户的签名 token，拼入终端命令。

**需要登录态（SSO）**

**Response：**
```json
{ "token": "abc123..." }
```

---

### GET `/ulivepde/api/verify/checkin`

用户终端运行命令时，curl 自动调用此接口上报版本信息。

**Query 参数：**

| 参数 | 说明 |
|------|------|
| mis | 用户 mis 号 |
| token | 签名 token（防伪造） |
| stage | 关卡号（1/2/3） |
| v | 版本号（第1关 catpaw 版本，第3关 claude 版本） |
| node | Node 版本（第2关） |
| git | Git 版本（第2关） |

**Response：** 纯文本 `ok` 或 `error: ...`

---

### POST `/ulivepde/api/progress/verify`（已有接口，扩展了 curl 类型）

前端点"我跑完了，提交校验"时调用。

**Request Body：**
```json
{ "stageId": 1 }
```

**curl 类型处理逻辑：**
- 查询该用户在过去 **10分钟内** 是否有 checkin 上报记录
- 有 → `{ "stageCompleted": true, "verifyStatus": "passed" }`
- 无 → `{ "stageCompleted": false, "verifyStatus": "failed", "message": "未检测到命令执行记录..." }`

---

## 本地验证（连 VPN 后）

```bash
# 1. 获取 token（需要登录态，本地用 mock_mis）
curl "http://localhost:8081/ulivepde/api/verify/token?mock_mis=yanli06"

# 2. 模拟 checkin 上报（把上面拿到的 token 替换进去）
curl "http://localhost:8081/ulivepde/api/verify/checkin?stage=1&mis=yanli06&token=TOKEN&v=1.2.3"

# 3. 提交校验
curl -X POST "http://localhost:8081/ulivepde/api/progress/verify?mock_mis=yanli06" \
  -H "Content-Type: application/json" \
  -d '{"stageId": 1}'
```
