# Search-Service (智能搜索与 AI 知识库服务)

基于 **Spring Boot 3 + Elasticsearch 8 + LangChain4j + Apache Tika + RocketMQ** 的企业级智能搜索服务。

## 1. 核心定位

- **统一索引 (Unified Index)**：聚合 IM 消息、协同文档、上传文件等多源异构数据
- **权限卫士 (Security Guard)**：Read-Time Filtering，确保用户只能搜到有权看到的内容
- **AI 大脑 (AI Brain)**：作为 RAG 引擎，为 ChatBot 提供私有知识上下文

## 2. 技术栈

- **Java 17** + Spring Boot 3.5.7
- **Elasticsearch 8.15.0**（混合检索：BM25 + 向量）
- **LangChain4j 0.33.0**（Embedding + RAG 编排）
- **Apache Tika 2.9.2**（PDF/Office 文档解析）
- **RocketMQ 2.2.3**（事件驱动的索引更新）

## 3. 架构与数据流

```
IM-message-server ──┐
                    ├──> RocketMQ ──> IngestListener ──> Embedding ──> Elasticsearch
Doc-server ─────────┘                                                    │
                                                                          │
User ──> SearchController ──> SearchService ──> ES (权限过滤) ──> 结果 ──┘
User ──> RagController ──> RagService ──> Retrieve + (TODO: LLM) ──> 答案
```

## 4. 核心功能模块

### 4.1 IM 消息索引（IngestListener）

- **Topic**: `IM_MSG_SEND`
- **索引**: `idx_im_messages`
- **流程**:
  1. 消费 RocketMQ 消息事件 (`MsgEvent`)
  2. 调用 `EmbeddingModel.embed(text)` 生成向量
  3. 写入 ES：`msgId`, `channelId`, `senderId`, `content`, `vector`, `createdAt`

### 4.2 文档索引 + Chunking（DocUpdateListener）

- **Topic**: `DOC_UPDATE`
- **索引**: `idx_documents`
- **流程**:
  1. 消费文档更新事件 (`DocUpdateEvent`)
  2. 如果有 `fileUrl`，则从 MinIO/HTTP 下载文件
  3. 使用 **Apache Tika** 提取纯文本
  4. **Chunking**：按 1000 字符切片，200 字符重叠
  5. 对每个 chunk 生成 embedding 向量
  6. 写入 ES：`chunkId`, `parentDocId`, `title`, `contentChunk`, `vector`, `allowUsers`, `allowDepts`, `isPublic`

### 4.3 权限感知搜索（DocumentSearchService）

- **接口**: `GET /api/search/documents?q=项目预算&userId=1001`
- **权限过滤（Read-Time Filtering）**:
  - 调用 `PermissionService` 获取用户的 `channelIds` / `deptIds`
  - 构建 ES bool 查询：
    ```json
    {
      "bool": {
        "must": [{"match": {"contentChunk": "项目预算"}}],
        "filter": [{
          "bool": {
            "should": [
              {"term": {"isPublic": true}},
              {"terms": {"allowUsers": [1001]}},
              {"terms": {"allowDepts": [10, 50]}}
            ]
          }
        }]
      }
    }
    ```
- **优势**：在底层 Lucene 扫描前就过滤掉无权文档，性能极高且绝对安全

### 4.4 RAG 问答接口骨架（RagController）

- **接口**: `POST /api/search/ask`
- **请求体**:
  ```json
  {
    "question": "飞书科技2024年的营收是多少？",
    "userId": 1001,
    "channelIds": [100, 102]
  }
  ```
- **流程**:
  1. **Retrieve**: 从 `idx_im_messages` 和 `idx_documents` 检索 TopK 相关片段
  2. **Assemble Context**: 组装上下文文本
  3. **Generate (TODO)**: 当前返回原文，后续可接入 LangChain4j 的 `ChatLanguageModel` 调用 LLM 生成答案

## 5. 配置说明

### 5.1 `application.yml`

```yaml
spring:
  application:
    name: IM-search-service
server:
  port: 8085

elasticsearch:
  host: http://localhost:9200

rocketmq:
  name-server: 127.0.0.1:9876

ai:
  embedding:
    api-key: YOUR_OPENAI_OR_COMPATIBLE_API_KEY
    model: text-embedding-3-small
```

### 5.2 前置要求

- **JDK 17**（在 IDE 中为 IM-search-server 模块配置 Java 17）
- **Elasticsearch 8.x** 已启动（`localhost:9200`）
- **RocketMQ NameServer + Broker** 已启动
- **OpenAI 兼容的 Embedding API**（或本地模型服务）

### 5.3 创建 ES 索引

在 ES 中创建两个索引：

**idx_im_messages**:
```bash
curl -X PUT "http://localhost:9200/idx_im_messages" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "msgId": {"type": "keyword"},
      "channelId": {"type": "long"},
      "senderId": {"type": "long"},
      "content": {"type": "text", "analyzer": "standard"},
      "vector": {"type": "dense_vector", "dims": 1536, "index": true, "similarity": "cosine"},
      "createdAt": {"type": "date"}
    }
  }
}'
```

**idx_documents**:
```bash
curl -X PUT "http://localhost:9200/idx_documents" -H "Content-Type: application/json" -d '{
  "mappings": {
    "properties": {
      "chunkId": {"type": "keyword"},
      "parentDocId": {"type": "long"},
      "title": {"type": "text"},
      "contentChunk": {"type": "text"},
      "vector": {"type": "dense_vector", "dims": 1536, "index": true, "similarity": "cosine"},
      "allowUsers": {"type": "long"},
      "allowDepts": {"type": "long"},
      "isPublic": {"type": "boolean"},
      "createdAt": {"type": "date"}
    }
  }
}'
```

## 6. 启动与测试

### 6.1 启动服务

```bash
cd IM-search-server
mvn spring-boot:run
```

或在 IDE 中运行：`org.example.imsearchservice.ImSearchServiceApplication`

### 6.2 测试 IM 消息搜索

手动插入测试数据：
```bash
curl -X PUT "http://localhost:9200/idx_im_messages/_doc/test-1" ^
  -H "Content-Type: application/json" ^
  -d "{\"msgId\":\"test-1\",\"channelId\":100,\"senderId\":1,\"content\":\"这是一个关于项目预算的测试消息\",\"createdAt\":\"2025-01-01T00:00:00Z\"}"
```

调用搜索接口：
```bash
curl "http://localhost:8085/api/search/messages?q=项目预算&channelIds=100"
```

### 6.3 测试文档搜索（带权限过滤）

插入测试文档：
```bash
curl -X PUT "http://localhost:9200/idx_documents/_doc/doc-1-0" ^
  -H "Content-Type: application/json" ^
  -d "{\"chunkId\":\"doc-1-0\",\"parentDocId\":1,\"title\":\"财报\",\"contentChunk\":\"2024年营收50亿\",\"allowUsers\":[1001],\"isPublic\":false,\"createdAt\":\"2025-01-01T00:00:00Z\"}"
```

调用搜索接口：
```bash
curl "http://localhost:8085/api/search/documents?q=营收&userId=1001"
```

### 6.4 测试 RAG 问答（演示版）

```bash
curl -X POST "http://localhost:8085/api/search/ask" ^
  -H "Content-Type: application/json" ^
  -d "{\"question\":\"2024年营收是多少？\",\"userId\":1001,\"channelIds\":[100]}"
```

返回示例：
```json
{
  "question": "2024年营收是多少？",
  "contexts": [
    "[文档:财报] 2024年营收50亿"
  ],
  "answer": "[演示版本] 请基于以上 1 条上下文信息自行判断答案。后续可接入 LLM 自动生成。"
}
```

## 7. 后续扩展点

- [ ] **向量 kNN 检索**：在 SearchService 中增加 `knn` 查询，实现混合检索（BM25 + 向量）
- [ ] **Reranker**：接入 bge-reranker 对 TopK 结果精细打分
- [ ] **LLM 集成**：在 RagService 中接入 LangChain4j 的 `ChatLanguageModel`，调用 GPT-4/DeepSeek 生成答案
- [ ] **Bulk 写入优化**：IngestListener 改为批量写入 ES，提升吞吐量
- [ ] **MCP Server**：实现 Model Context Protocol，为 AI Agent 提供工具调用能力

## 8. 模块依赖说明

当前 `IM-search-server` 模块使用独立的 Spring Boot parent（3.5.7 + Java 17），与父 pom（Spring Boot 2.7.18 + Java 8）的其他模块隔离。

如果遇到 **"build path error"**，请在 IDE 中：
1. 右键 `IM-search-server` 模块 → Properties → Java Build Path
2. 设置 JDK 为 Java 17

---

**search-service 现已完成基础架构搭建，可直接启动并对接 IM 消息和文档的实时索引与智能检索！**
