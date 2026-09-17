# ********************************************************************
# fault-patrol-agent 初始化 SQL（MySQL 8.x）
# 数据库：fault_patrol_agent
# 说明：建表 + 巡检诊断场景种子数据（四阶段 Plan-and-Execute 智能体、
#       巡检工具 MCP 配置、故障手册 RAG Advisor、定时巡检任务）
# ********************************************************************

SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS `fault_patrol_agent` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `fault_patrol_agent`;

# ------------------------------------------------------------
# 管理员用户表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` varchar(64) NOT NULL COMMENT '用户ID（唯一标识）',
  `username` varchar(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用,2:锁定)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员用户表';

# 默认账号 admin / admin123（生产环境请立即修改）
INSERT INTO `admin_user` (`id`, `user_id`, `username`, `password`, `status`)
VALUES (1, '10001', 'admin', 'admin123', 1);

# ------------------------------------------------------------
# AI 智能体配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent`;
CREATE TABLE `ai_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `agent_name` varchar(50) NOT NULL COMMENT '智能体名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `channel` varchar(32) DEFAULT NULL COMMENT '渠道类型(agent，chat_stream)',
  `strategy` varchar(64) DEFAULT NULL COMMENT '执行策略Bean名(diagnoseAgentExecuteStrategy、flowAgentExecuteStrategy、fixedAgentExecuteStrategy)',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_id` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI智能体配置表';

INSERT INTO `ai_agent` (`id`, `agent_id`, `agent_name`, `description`, `channel`, `strategy`, `status`)
VALUES
  (1, '10001', '业务故障巡检诊断 Agent', 'Plan-and-Execute 四阶段故障诊断：故障分析规划 → 多工具取证 → 证据质量监督 → 诊断报告', 'chat_stream', 'diagnoseAgentExecuteStrategy', 1),
  (2, '10002', '定时巡检 Agent（固定链）', '多客户端串联对话的轻量巡检模式，适合定时巡检任务', 'agent', 'fixedAgentExecuteStrategy', 1);

# ------------------------------------------------------------
# 智能体-客户端流程配置表（四阶段）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_flow_config`;
CREATE TABLE `ai_agent_flow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_name` varchar(64) DEFAULT NULL COMMENT '客户端名称',
  `client_type` varchar(64) DEFAULT NULL COMMENT '客户端类型(PLAN_CLIENT/EVIDENCE_CLIENT/SUPERVISION_CLIENT/REPORT_CLIENT)',
  `sequence` int NOT NULL COMMENT '序列号(执行顺序)',
  `step_prompt` text COMMENT '步骤提示词',
  `status` int DEFAULT '1' COMMENT '状态；0无效，1有效',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_client_seq` (`agent_id`,`client_id`,`sequence`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体-客户端关联表';

INSERT INTO `ai_agent_flow_config` (`agent_id`, `client_id`, `client_name`, `client_type`, `sequence`, `step_prompt`, `status`)
VALUES
  ('10001', '9101', '规划-故障分析', 'PLAN_CLIENT', 1, '**原始告警:** %s\n**当前执行步骤:** 第 %d 步 (最大 %d 步)\n**历史执行记录:**\n%s\n**当前任务:** %s\n\n你是资深 SRE 故障诊断专家。请对告警进行故障分析并制定取证计划：\n1. 分析告警现象与可能的故障方向（如缓存、MQ、数据库、依赖服务等）\n2. 评估已有取证记录的完整度，识别证据缺口\n3. 制定下一步取证策略：明确需要调用的巡检工具（Prometheus 指标、Jaeger 链路、业务数据、Redis、MQ、容器）及查询目标\n4. 评估当前诊断完成度\n\n**输出格式要求:**\n故障分析: [告警现象与可能的根因方向分析]\n执行历史评估: [对已完成取证的质量和效果评估]\n取证策略: [下一步需要调用的工具与查询目标]\n完成度评估: [0-100]%%\n任务状态: [CONTINUE/COMPLETED]', 1),
  ('10001', '9201', '取证-多工具协同', 'EVIDENCE_CLIENT', 2, '**原始告警:** %s\n**规划策略:** %s\n\n你是故障取证执行器。严格按规划策略调用可用的 MCP 巡检工具（Prometheus 指标查询、Jaeger 链路追踪、业务数据查询、Redis 检查、MQ 队列检查、容器状态）完成交叉取证：\n1. 指标维度：查询相关服务的黄金指标（QPS、错误率、延迟、资源水位）\n2. 链路维度：检索相关服务的慢链路与错误链路，定位异常节点\n3. 业务数据维度：核对告警时段内的业务数据异常（如订单、库存、支付记录）\n4. 中间件维度：检查 Redis 内存/慢日志、MQ 队列积压、容器资源\n5. 交叉验证：综合各维度证据，收敛根因方向\n\n**输出格式:**\n取证目标: [本步取证要回答的问题]\n取证过程: [实际调用的工具与关键参数]\n取证结果: [获得的证据数据，含指标数值、链路特征、业务数据]\n证据检查: [证据的完整性与可信度评估]', 1),
  ('10001', '9301', '监督-证据质检', 'SUPERVISION_CLIENT', 3, '**原始告警:** %s\n**取证结果:** %s\n\n你是诊断质量监督员。严格评估本轮取证是否足以定位根因：\n1. 证据是否覆盖指标/链路/业务数据等多维度\n2. 根因方向是否收敛、是否有反例未被解释\n3. 若证据不足，指出缺失维度并给出重新取证的改进建议\n\n**输出格式:**\n质量评估: [对本轮取证质量的总体评估]\n问题识别: [证据缺口与待确认问题]\n改进建议: [下一步取证的具体建议]\n质量评分: [1-10分]\n是否通过: [PASS/FAIL/OPTIMIZE]', 1),
  ('10001', '9401', '报告-诊断输出', 'REPORT_CLIENT', 4, '基于以下取证过程，输出结构化故障诊断报告：\n**原始告警:** %s\n**取证过程与证据:**\n%s\n\n**输出格式要求:**\n## 故障概述\n[故障现象、影响范围与时间线]\n## 根因分析\n[基于证据链的根因定位结论，注明依据的指标/链路/业务数据]\n## 处置建议\n[按优先级排列的可执行处置步骤，含回滚方案]\n## 预防措施\n[避免复发的监控与治理建议]', 1),
  ('10002', '9101', '巡检-分析', 'DEFAULT', 1, '', 1),
  ('10002', '9201', '巡检-取证', 'DEFAULT', 2, '', 1);

# ------------------------------------------------------------
# AI 客户端配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client`;
CREATE TABLE `ai_client` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `client_id` varchar(64) NOT NULL COMMENT '客户端ID',
  `client_name` varchar(64) NOT NULL COMMENT '客户端名称',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_client_id` (`client_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI客户端配置表';

INSERT INTO `ai_client` (`client_id`, `client_name`, `description`, `status`)
VALUES
  ('9101', '规划客户端', '四阶段-故障分析规划', 1),
  ('9201', '取证客户端', '四阶段-多工具取证执行（绑定巡检工具 MCP 与故障手册 RAG）', 1),
  ('9301', '监督客户端', '四阶段-证据质量监督', 1),
  ('9401', '报告客户端', '四阶段-诊断报告生成', 1);

# ------------------------------------------------------------
# AI API 配置表（OpenAI 兼容协议）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_api`;
CREATE TABLE `ai_client_api` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `api_id` varchar(64) NOT NULL COMMENT 'API ID',
  `base_url` varchar(255) NOT NULL COMMENT 'API地址',
  `api_key` varchar(255) NOT NULL COMMENT 'API密钥',
  `completions_path` varchar(128) DEFAULT NULL COMMENT '对话补全路径',
  `embeddings_path` varchar(128) DEFAULT NULL COMMENT '向量嵌入路径',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_id` (`api_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI API配置表';

# api_key 请替换为你的密钥（DeepSeek 等任意 OpenAI 兼容服务均可）
INSERT INTO `ai_client_api` (`api_id`, `base_url`, `api_key`, `completions_path`, `embeddings_path`, `status`)
VALUES
  ('8001', 'https://api.deepseek.com', 'sk-your-llm-api-key', '/chat/completions', '/embeddings', 1);

# ------------------------------------------------------------
# AI 模型配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_model`;
CREATE TABLE `ai_client_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `model_id` varchar(64) NOT NULL COMMENT '模型ID',
  `api_id` varchar(64) NOT NULL COMMENT 'API ID',
  `model_name` varchar(128) NOT NULL COMMENT '模型名称',
  `model_type` varchar(32) DEFAULT NULL COMMENT '模型类型(openai/deepseek/claude)',
  `model_usage` varchar(255) DEFAULT NULL COMMENT '用途说明',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI模型配置表';

INSERT INTO `ai_client_model` (`model_id`, `api_id`, `model_name`, `model_type`, `model_usage`, `status`)
VALUES
  ('8101', '8001', 'deepseek-chat', 'openai', '规划/监督/报告通用模型', 1),
  ('8201', '8001', 'deepseek-chat', 'openai', '取证执行模型（绑定巡检工具 MCP）', 1);

# ------------------------------------------------------------
# MCP 工具配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_tool_mcp`;
CREATE TABLE `ai_client_tool_mcp` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mcp_id` varchar(64) NOT NULL COMMENT 'MCP ID',
  `mcp_name` varchar(64) NOT NULL COMMENT 'MCP名称',
  `transport_type` varchar(16) NOT NULL COMMENT '传输类型(sse/stdio)',
  `transport_config` text COMMENT '传输配置JSON',
  `request_timeout` int DEFAULT '120' COMMENT '请求超时（秒）',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mcp_id` (`mcp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MCP工具配置表';

# 巡检工具 MCP 服务器（fault-patrol-agent-mcp-server 模块）
# 注意：docker compose 环境下使用容器网络地址 http://mcp-server:8092；
#       本地开发（本机直跑两个 jar）时请改为 http://localhost:8092
INSERT INTO `ai_client_tool_mcp` (`mcp_id`, `mcp_name`, `transport_type`, `transport_config`, `request_timeout`, `status`)
VALUES
  ('9001', 'inspect-tools', 'sse', '{"baseUri":"http://mcp-server:8092","sseEndpoint":"/sse"}', 120, 1);

# ------------------------------------------------------------
# Advisor 顾问配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_advisor`;
CREATE TABLE `ai_client_advisor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `advisor_id` varchar(64) NOT NULL COMMENT '顾问ID',
  `advisor_name` varchar(64) NOT NULL COMMENT '顾问名称',
  `advisor_type` varchar(64) NOT NULL COMMENT '顾问类型(ChatMemory/RagAnswer)',
  `order_num` int DEFAULT '1' COMMENT '执行顺序',
  `ext_param` text COMMENT '扩展参数JSON',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_advisor_id` (`advisor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='顾问配置表';

INSERT INTO `ai_client_advisor` (`advisor_id`, `advisor_name`, `advisor_type`, `order_num`, `ext_param`, `status`)
VALUES
  ('7001', '多轮诊断记忆', 'ChatMemory', 1, '{"maxMessages": 50}', 1),
  ('7002', '故障手册知识库', 'RagAnswer', 2, '{"topK": 4, "similarityThreshold": 0.35, "filterExpression": "knowledge == \'fault-handbook\'"}', 1);

# ------------------------------------------------------------
# 系统提示词配置表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_system_prompt`;
CREATE TABLE `ai_client_system_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `prompt_id` varchar(64) NOT NULL COMMENT '提示词ID',
  `prompt_name` varchar(64) NOT NULL COMMENT '提示词名称',
  `prompt_content` text COMMENT '提示词内容',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prompt_id` (`prompt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统提示词配置表';

INSERT INTO `ai_client_system_prompt` (`prompt_id`, `prompt_name`, `prompt_content`, `description`, `status`)
VALUES
  ('6001', '巡检诊断助手', '你是业务系统智能故障巡检 Agent，面向业务系统故障定位场景：基于告警信息与可观测数据（指标、链路、业务数据），通过规划-取证-监督-总结的协作流程完成故障定位。你的职责：1) 制定取证计划；2) 调用 MCP 巡检工具交叉取证；3) 输出含根因分析与处置建议的结构化诊断报告。原则：基于证据说话，不臆测；所有工具调用均为只读操作。', '巡检诊断助手系统提示词', 1);

# ------------------------------------------------------------
# 通用关联配置表（client/model/prompt/tool_mcp/advisor 装配关系）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_config`;
CREATE TABLE `ai_client_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `source_type` varchar(32) NOT NULL COMMENT '来源类型(client/model)',
  `source_id` varchar(64) NOT NULL COMMENT '来源ID',
  `target_type` varchar(32) NOT NULL COMMENT '目标类型(client/model/prompt/tool_mcp/advisor/api)',
  `target_id` varchar(64) NOT NULL COMMENT '目标ID',
  `ext_param` text COMMENT '扩展参数JSON',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source_type`,`source_id`),
  KEY `idx_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用关联配置表';

INSERT INTO `ai_client_config` (`source_type`, `source_id`, `target_type`, `target_id`, `status`)
VALUES
  # 规划客户端：通用模型 + 系统提示词 + 多轮记忆
  ('client', '9101', 'model', '8101', 1),
  ('client', '9101', 'prompt', '6001', 1),
  ('client', '9101', 'advisor', '7001', 1),
  # 取证客户端：取证模型 + 巡检工具 MCP + 故障手册 RAG + 多轮记忆
  ('client', '9201', 'model', '8201', 1),
  ('client', '9201', 'prompt', '6001', 1),
  ('client', '9201', 'advisor', '7001', 1),
  ('client', '9201', 'advisor', '7002', 1),
  ('client', '9201', 'tool_mcp', '9001', 1),
  # 监督客户端
  ('client', '9301', 'model', '8101', 1),
  ('client', '9301', 'advisor', '7001', 1),
  # 报告客户端
  ('client', '9401', 'model', '8101', 1),
  ('client', '9401', 'advisor', '7001', 1),
  # 取证模型绑定巡检工具 MCP
  ('model', '8201', 'tool_mcp', '9001', 1);

# ------------------------------------------------------------
# 智能体任务调度表（定时巡检）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_agent_task_schedule`;
CREATE TABLE `ai_agent_task_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `agent_id` varchar(64) NOT NULL COMMENT '智能体ID',
  `task_name` varchar(128) NOT NULL COMMENT '任务名称',
  `description` varchar(255) DEFAULT NULL COMMENT '任务描述',
  `cron_expression` varchar(64) NOT NULL COMMENT 'cron表达式',
  `task_param` text COMMENT '任务参数JSON',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='智能体任务调度表';

# 每 30 分钟执行一次定时巡检
INSERT INTO `ai_agent_task_schedule` (`agent_id`, `task_name`, `description`, `cron_expression`, `task_param`, `status`)
VALUES
  ('10001', '定时巡检任务', '每30分钟对业务系统执行一次故障巡检诊断', '0 0/30 * * * ?', '{"message":"执行定时巡检任务：检查业务系统各项指标（错误率、响应时间、资源水位、MQ积压、缓存状态）是否存在异常"}', 1);

# ------------------------------------------------------------
# 知识库工单表（RAG 入库台账）
# ------------------------------------------------------------
DROP TABLE IF EXISTS `ai_client_rag_order`;
CREATE TABLE `ai_client_rag_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rag_id` varchar(64) DEFAULT NULL COMMENT '知识库ID',
  `rag_name` varchar(128) NOT NULL COMMENT '知识库名称',
  `knowledge_tag` varchar(64) NOT NULL COMMENT '知识标签',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态(0:禁用,1:启用)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='知识库工单表';

# ------------------------------------------------------------
# 诊断报告表
# ------------------------------------------------------------
DROP TABLE IF EXISTS `diagnosis_report`;
CREATE TABLE `diagnosis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(128) NOT NULL COMMENT '会话ID（一次诊断任务唯一标识）',
  `agent_id` varchar(64) DEFAULT NULL COMMENT '智能体ID',
  `alert_content` longtext COMMENT '告警内容（原始诊断输入）',
  `root_cause` longtext COMMENT '根因分析',
  `remediation` longtext COMMENT '处置建议',
  `evidence` longtext COMMENT '取证过程（证据链）',
  `summary` longtext COMMENT '完整诊断总结',
  `status` varchar(32) DEFAULT NULL COMMENT '诊断状态：COMPLETED/STEP_LIMIT',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='诊断报告表';

# ********************************************************************
