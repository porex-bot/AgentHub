# AgentHub - 多智能体 AI 工作台

AgentHub 是一个全栈 AI 智能体工作台，面向关系咨询、复杂任务执行和资料研究报告生成等场景。项目提供统一的聊天入口，支持 SSE 流式响应、会话记忆、RAG 检索增强、工具调用、MCP 工具集成和 PDF 报告导出。

## 项目结构

```text
AgentHub/
├── AgentHub-backend/                         # Spring Boot 后端服务
│   ├── src/main/java/com/agenthub/server/
│   │   ├── agent/                            # 智能体核心逻辑
│   │   ├── app/                              # 应用服务层
│   │   ├── chatMemory/                       # 文件化会话记忆
│   │   ├── config/                           # 后端配置
│   │   ├── controller/                       # API 控制器
│   │   ├── rag/                              # RAG 检索增强
│   │   └── tools/                            # 工具调用能力
│   ├── src/main/resources/
│   │   ├── markdowns/                        # 关系咨询知识库文档
│   │   └── mcp-servers.example.json          # MCP 配置示例
│   └── agenthub-image-search-mcp-server/     # 图片搜索 MCP 服务
└── AgentHub-frontend/                        # Vue 3 前端工作台
    └── src/
        ├── components/                       # 页面组件
        ├── config/                           # 智能体应用配置
        ├── services/                         # HTTP 和 SSE 客户端
        └── views/                            # 页面视图
```

## 核心功能

### 关系咨询助手

用于恋爱、婚姻、亲密关系和沟通冲突等场景，结合内置领域知识库，提供关系分析、沟通建议和长期对话记忆。

### 通用任务智能体

用于复杂目标拆解和任务执行。智能体会基于 ReAct 思路进行多步骤推理，并按需调用搜索、网页抓取、文件操作、PDF 生成等工具。

### 研究报告助手

用于资料检索、主题研究和结构化报告生成。支持网页搜索、内容整理、报告撰写，并可将报告导出为 PDF 文件。

### 会话记忆

后端使用文件化聊天记忆保存上下文，每个 `chatId` 对应独立会话文件。上线部署时建议将会话目录配置到持久化磁盘，避免写入系统临时目录。

### 流式对话

主要聊天接口使用 SSE 返回内容，前端可以实时展示智能体的输出过程和执行步骤。

## 技术栈

后端：

- Java 21
- Spring Boot 3.3.5
- Spring AI
- Alibaba DashScope
- PGVector
- Kryo
- Jsoup
- iText PDF
- Sa-Token
- Knife4j / SpringDoc

前端：

- Vue 3
- Vite
- Vue Router
- Axios
- EventSource
- Lucide Vue Next

## 环境配置

真实密钥不会提交到仓库。请通过环境变量或本地私有配置文件提供。

必需环境变量：

```bash
DASHSCOPE_API_KEY=your_dashscope_api_key
SEARCH_API_KEY=your_search_api_key
```

可选环境变量：

```bash
DASHSCOPE_CHAT_MODEL=qwen-plus-0112
AMAP_MAPS_API_KEY=your_amap_api_key
AGENTHUB_CHAT_MEMORY_BASE_DIR=/data/agenthub/chat-memory
```

上线安全相关环境变量：

```bash
AGENTHUB_API_TOKEN=your_private_api_token
AGENTHUB_AI_RATE_LIMIT_PER_MINUTE=60
AGENTHUB_CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
SPRINGDOC_ENABLED=false
KNIFE4J_ENABLED=false
```

如果配置了 `AGENTHUB_API_TOKEN`，后端 AI 接口会要求请求携带：

```text
X-AgentHub-Token: your_private_api_token
```

或：

```text
Authorization: Bearer your_private_api_token
```

如果前端通过 Nginx 反向代理访问后端，建议由 Nginx 在转发到后端时注入该请求头，不要把生产 Token 写进公开前端代码。

MCP 配置示例文件：

```text
AgentHub-backend/src/main/resources/mcp-servers.example.json
```

如需启用本地 MCP 配置，请复制为：

```text
AgentHub-backend/src/main/resources/mcp-servers.json
```


## 本地启动

启动后端：

```bash
cd AgentHub-backend
mvn spring-boot:run
```

启动前端：

```bash
cd AgentHub-frontend
npm install
npm run dev
```

启动图片搜索 MCP 服务：

```bash
cd AgentHub-backend/agenthub-image-search-mcp-server
mvn spring-boot:run
```

## API 接口

后端默认地址：

```text
http://localhost:8123/api
```

主要接口：

```text
GET /api/ai/agents/relationship/chat/stream
GET /api/ai/agents/task/chat/stream
GET /api/ai/agents/research/chat/stream
GET /api/ai/files/pdf/{fileName}
```

## 验证命令

后端测试：

```bash
cd AgentHub-backend
mvn clean "-Dtest=AiControllerMappingTest,TaskExecutionAgentToolFilteringTest,ResearchReportAgentTest,ResourceDownloadToolTest" test
```

前端测试和构建：

```bash
cd AgentHub-frontend
node --test src/services/workflowSteps.test.mjs
npm run build
```

## 上线提醒

当前仓库已经加入基础上线保护：

- AI 和 PDF 接口支持私有 Token 保护。
- AI 接口支持按客户端地址限流。
- CORS 默认只允许本地开发地址，生产环境应配置为明确域名。
- Swagger / Knife4j 默认关闭。
- 生产日志默认不再以 debug 级别打印聊天 advisor 内容。
- Web 搜索工具使用默认 SSL 证书校验。
- `chatId` 增加格式校验，避免路径穿越写入会话文件。

这些保护适合作为基础防线。正式公网产品仍建议接入完整登录体系、用户级配额、网关限流、监控告警和数据清理策略。

## 许可证

本项目使用 MIT License。
