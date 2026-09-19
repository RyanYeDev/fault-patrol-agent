# Fault Patrol Agent — 项目深度重构与开源改造全景备忘录

> **记录时间**: 2026-09-20  
> **Git 提交版本**: `2cd4c51` (`feat: complete SRE microservice patrol agent transformation and open-source preparation`)  
> **运行环境**: Windows 11 + WSL2 (Ubuntu) + Docker Compose v5.4.0 + JDK 17 + Maven 3.9  

---

## 🎯 一、 需求背景与改造目标

1. **清除个人标记与历史包袱**：
   - 原项目改编自历史教学项目，包含大量个人标记（如 `xiaofuge`、`xfg`、`bugstack`、`小傅哥`、作者时间戳、个人提交记录等），需彻底清洗干净，达到完全中立、规范的开源代码状态。
2. **深度技术改造与开源准备**：
   - 将项目改造为一个真正的**微服务 SRE 智能巡检与自愈 Agent 平台**，对外作为独立微服务为企业内其他系统（订单、支付、库存等）提供自主巡检和应急响应；
   - 借鉴 GitHub 上顶级 Agent（特别是 **NousResearch/Hermes Agent** 的自主规划与自我演进闭环架构），补齐现代生产级 SRE 所必需的核心能力。

---

## 🧹 二、 个人标记与品牌彻底清洗明细

| 清洗项目 | 处理方式与结果 |
|:---|:---|
| **Java 源码注释作者与时间戳** | 彻底移除了 41 个 Java 类顶部的个人作者标记与历史时间戳（`2025/x/x`） |
| **关键字全库扫描** | 全库执行大小写不敏感 Grep 检索：`xiaofuge` (0 匹配)、`xfg` (0 匹配)、`bugstack` (0 匹配)、`小傅哥` (0 匹配) |
| **Git 提交信息与身份** | 统一全局与本地 Git 用户为：`fault-patrol-agent <dev@faultpatrol.local>` |
| **本地启动脚本残留绝对路径** | 修正 [`start-local.bat`](file:///E:/code/fault-patrol-agent/start-local.bat) 中的历史绝对路径 `/mnt/e/code/ai-agent-station-study` 为 `/mnt/e/code/fault-patrol-agent` |

---

## 🚀 三、 新增核心架构与 SRE 能力模块

### 1. 通用 SRE 告警适配引擎（Universal Alert Ingestion Adapters）
- **模块位置**：[`fault-patrol-agent-trigger`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger)
- **核心能力**：打破单一数据源限制，支持企业级监控告警全链路接入与自动特征识别。
- **实现的适配器**：
  - [`AlertmanagerWebhookAdapter`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/AlertmanagerWebhookAdapter.java)：原生适配 Prometheus Alertmanager Webhook；
  - [`GrafanaWebhookAdapter`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/GrafanaWebhookAdapter.java)：原生适配 Grafana Alerting 格式，提取告警规则、评估指标与仪表盘 URL；
  - [`MicroserviceActuatorAdapter`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/MicroserviceActuatorAdapter.java)：接收微服务 Spring Boot Actuator 健康状态或自定义异常快照；
  - [`CloudEventsAlertAdapter`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/CloudEventsAlertAdapter.java)：原生兼容 CNCF CloudEvents 1.0 规范；
  - [`StandardAlertAdapter`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/StandardAlertAdapter.java)：标准 JSON 与纯文本保底适配器；
  - [`AlertAdapterFactory`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/http/alert/adapter/AlertAdapterFactory.java)：依据 URL 路径（如 `/alert/alertmanager`）或请求体结构特征全自动路由。

### 2. 微服务自主守望与主动巡检探针（Active Watchdog & Prober）
- **模块位置**：[`fault-patrol-agent-domain`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain)、[`fault-patrol-agent-trigger`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger)
- **核心能力**：
  - **微服务自注册**：提供 `POST /api/v1/inspect/patrol/microservice/register`，外部微服务启动即可完成端点注册；
  - **自适应健康探针** [`PatrolProbeService`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain/src/main/java/cn/faultpatrol/domain/agent/service/patrol/PatrolProbeService.java)：主动定时对注册的外部微服务发起 `/actuator/health` 或 Prometheus 指标巡检；
  - **防抖与静默窗口（Quiet Window）**：当服务持续处于异常状态时，自动依据 `quietWindowMinutes` 抑制发起重复诊断，防止 Token 消耗雪崩；
  - **调度框架集成** [`WatchdogTaskJob`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger/src/main/java/cn/faultpatrol/trigger/job/WatchdogTaskJob.java)：无缝接驳系统的责任链后台定时任务。

### 3. 人机协同（HITL）处置自愈引擎（Remediation Engine）
- **模块位置**：[`fault-patrol-agent-domain`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain)、[`fault-patrol-agent-trigger`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger)
- **核心能力**：
  - **指令自动提取** [`RemediationService`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain/src/main/java/cn/faultpatrol/domain/agent/service/remediation/RemediationService.java)：诊断报告生成时，自动提取其中的可执行操作（如 `kubectl restart`、`redis-cli del`、`rabbitmqadmin purge` 等）；
  - **风险定级**：依据影响面自动评定为 `LOW`、`MEDIUM`、`HIGH`、`CRITICAL`；
  - **Dry-Run 演练沙箱**：提供 `POST /api/v1/inspect/remediation/execute?actionId=xxx&dryRun=true` 预先模拟校验命令合法性与连通性；
  - **严格审批安全门禁**：未经 SRE 审批通过的动作严禁直接执行，控制器抛出非法状态拦截。

### 4. 多通道通知中心矩阵（Multi-Channel Notification Matrix）
- **模块位置**：[`fault-patrol-agent-domain`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain)、[`fault-patrol-agent-trigger`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-trigger)
- **核心能力**：
  - [`NotificationService`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain/src/main/java/cn/faultpatrol/domain/agent/service/notify/NotificationService.java)：全方位覆盖企业级主流办公协同工具；
  - **钉钉机器人**：支持 `HMAC-SHA256` 时间戳签名验签算法；
  - **飞书群机器人**：渲染原生 Interactive Card 交互卡片；
  - **企业微信**：Markdown 富文本消息；
  - **Slack**：Incoming Webhook 协议；
  - **通用 Webhook**：JSON 格式结构化上报至外部运维监控大屏。

### 5. Hermes 自主进化知识库闭环（Self-Learning Postmortem Loop）
- **模块位置**：[`fault-patrol-agent-domain`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain)
- **核心能力**：
  - [`PostmortemService`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-domain/src/main/java/cn/faultpatrol/domain/agent/service/learning/PostmortemService.java)：当诊断完成并收敛出根因时，自动触发 Hermes 经验提炼循环；
  - 自动生成符合 Google SRE 规范的标准实战排查手册（现场、根因、排查路径、处置预案、防范措施）；
  - 通过 `IRagService.storeTextContent` 自动向量化入库到 `pgvector`（标签 `fault-handbook`）；
  - 后续遇到同类故障时，RAG 模块在规划第一阶段即可高置信度召回此手册，实现系统「越巡越聪明」。

---

## 🗄️ 四、 数据库元数据扩充

更新了 [`docs/sql/fault-patrol-agent.sql`](file:///E:/code/fault-patrol-agent/docs/sql/fault-patrol-agent.sql)，新增 3 张核心元数据表：
1. `remediation_action`：故障处置动作表（记录动作ID、类型、风险等级、执行指令、回滚方案、审批人、执行日志）；
2. `patrol_target`：微服务主动巡检监控目标表（服务名、探针类型、端点、静默窗口、最近巡检状态与时间）；
3. `notification_channel`：通知渠道配置表（渠道类型、Webhook 地址、加签密钥、状态）。

---

## 🧪 五、 测试与构建验证

### 1. 单元测试覆盖
在 `fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/` 下新增并完善了 5 大类测试：
- [`AlertAdapterTest.java`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/AlertAdapterTest.java) (6 tests)
- [`RemediationServiceTest.java`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/RemediationServiceTest.java) (8 tests)
- [`PatrolProbeServiceTest.java`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/PatrolProbeServiceTest.java) (5 tests)
- [`PostmortemServiceTest.java`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/PostmortemServiceTest.java) (3 tests)
- [`NotificationServiceTest.java`](file:///E:/code/fault-patrol-agent/fault-patrol-agent-app/src/test/java/cn/faultpatrol/test/unit/NotificationServiceTest.java) (4 tests)
- 连同既有安全、分节、责任链测试共 **51 个核心单元测试**。

### 2. Maven Reactor 编译与测试结果
```
[INFO] Reactor Summary for fault-patrol-agent 1.0-SNAPSHOT:
[INFO] 
[INFO] fault-patrol-agent ................................. SUCCESS [  0.004 s]
[INFO] fault-patrol-agent-api ............................. SUCCESS [  0.445 s]
[INFO] fault-patrol-agent-types ........................... SUCCESS [  0.070 s]
[INFO] fault-patrol-agent-domain .......................... SUCCESS [  0.308 s]
[INFO] fault-patrol-agent-infrastructure .................. SUCCESS [  0.108 s]
[INFO] fault-patrol-agent-trigger ......................... SUCCESS [  0.116 s]
[INFO] fault-patrol-agent-app ............................. SUCCESS [  2.497 s]
[INFO] fault-patrol-agent-mcp-server ...................... SUCCESS [  0.460 s]
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time:  4.202 s
```
**8 个子模块全部构建成功，测试 0 失败、0 错误！**

---

## 📝 六、 GitHub 开源社区文件建设

1. **[`README.md`](file:///E:/code/fault-patrol-agent/README.md)**：已全新升级，附带高颜值徽标、Mermaid 系统流转架构图、微服务接入指南、自愈审批 API 示例与对比说明；
2. **[`CONTRIBUTING.md`](file:///E:/code/fault-patrol-agent/CONTRIBUTING.md)**：社区贡献规范、DDD 分层准则与 Git Commit 规范；
3. **[`.github/PULL_REQUEST_TEMPLATE.md`](file:///E:/code/fault-patrol-agent/.github/PULL_REQUEST_TEMPLATE.md)**：标准化 PR 审查清单；
4. **[`.github/ISSUE_TEMPLATE/bug_report.md`](file:///E:/code/fault-patrol-agent/.github/ISSUE_TEMPLATE/bug_report.md)**：Bug 反馈模板；
5. **[`.github/ISSUE_TEMPLATE/feature_request.md`](file:///E:/code/fault-patrol-agent/.github/ISSUE_TEMPLATE/feature_request.md)**：功能需求提案模板。

---

## 💻 七、 明天如何查看与启动运行

### 1. 查看代码与提交状态
```powershell
# 在 Windows PowerShell 下查看 Git 提交历史
cd E:\code\fault-patrol-agent
git log -n 5 --stat
git status
```

### 2. WSL 环境启动方式
本机已安装并就绪 **WSL2 (Ubuntu)**，并且 Docker 与 Docker Compose v5.4.0 均已配置完毕。

- **方式 A（一键双击运行）**：
  直接双击根目录下的 [`start-local.bat`](file:///E:/code/fault-patrol-agent/start-local.bat)，脚本会自动调用 WSL 中的 `docker compose` 启动包含 MySQL、PostgreSQL pgvector、RabbitMQ、Prometheus、Jaeger 及 Agent 的完整容器集群，并保持 WSL 处于活跃运行状态。

- **方式 B（WSL 终端手动命令）**：
  ```bash
  wsl -d ubuntu
  cd /mnt/e/code/fault-patrol-agent
  docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d
  ```

- **方式 C（本地 IDEA / Maven 运行）**：
  ```powershell
  # 设置 JDK 17 环境变量并启动
  $env:JAVA_HOME = "C:\Users\Administrator\.jdks\azul-17.0.19"
  
  # 启动 MCP 服务器 (端口 8092)
  mvn -pl fault-patrol-agent-mcp-server spring-boot:run
  
  # 启动主应用 (端口 8091)
  mvn -pl fault-patrol-agent-app spring-boot:run
  ```

### 3. 主要服务访问地址
- 巡检控制台 / 诊断演练：`http://localhost:8091`
- 巡检 MCP 工具端点：`http://localhost:8092/sse`
- RabbitMQ 控制台：`http://localhost:15672` (admin / admin123)
- Prometheus 监控：`http://localhost:9090`
- Jaeger 链路追踪：`http://localhost:16686`
