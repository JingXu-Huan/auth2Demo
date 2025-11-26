这是 `search-service`** (智能搜索与 AI 知识库服务)** 的详细架构设计。

在现代企业级协作平台中，搜索不再只是简单的“关键词匹配”，而是演变成了 **RAG (检索增强生成)** 和 **AI 助手** 的数据底座。本设计将融合 **Elasticsearch (全文/向量混合检索)** 与 **LLM (大语言模型)**，构建一个既懂业务又懂语义的智能中台。

---

### 1. 核心定位与设计哲学
+ **核心定位**：
    - **统一索引 (Unified Index)**：聚合 IM 消息、协同文档、上传文件、通讯录等多源异构数据。
    - **权限卫士 (Security Guard)**：确保“搜不到无权看的内容”（Security Trimming）。
    - **AI 大脑 (AI Brain)**：作为 RAG 引擎，为 ChatBot 提供私有知识上下文；作为 MCP Server，为 AI Agent 提供工具调用能力。
+ **设计哲学**：
    - **Hybrid Search (混合检索)**：关键词匹配 (BM25) 保障精确度，向量检索 (Embedding) 保障语义召回率。
    - **Read-Time Filtering (读时过滤)**：不把权限“展开”存储在索引中（写入太慢），而在查询时动态注入权限过滤条件。

---

### 2. 技术栈清单
+ **开发框架**: Spring Boot 3.x + LangChain4j (Java 版的 AI 编排框架)
+ **搜索引擎**: **Elasticsearch 8.x** (原生支持 kNN 向量搜索 + NLP 插件)
+ **向量模型**: `bge-m3` 或 `text-embedding-3-small` (根据数据隐私要求选择本地或云端)
+ **内容提取**: **Apache Tika** (解析 PDF, Word, Excel, PPT)
+ **大模型接口**: OpenAI API 标准接口 (对接 GPT-4, DeepSeek, Qwen 等)

---

### 3. 架构数据流图 (Architecture Diagram)
```mermaid
graph TD
    User[用户] --> Gateway
    Gateway --> SearchService

    subgraph Data Ingestion (写链路)
        MQ[RocketMQ] -- 1. 消费消息/文档 --> Ingester[Data Ingester]
        Ingester -- 2. 解析 (Tika) --> Parser[Doc Parser]
        Parser -- 3. 切片 (Chunking) --> Chunker
        Chunker -- 4. 向量化 (Embedding) --> EmbedModel[Embedding Model]
        EmbedModel -- 5. 写入 --> ES[(Elasticsearch)]
    end

    subgraph Query Pipeline (读链路)
        SearchService -- 1. 关键词/语义 --> QueryBuilder
        QueryBuilder -- 2. 注入权限 (ACL) --> SecurityFilter
        SecurityFilter -- 3. Hybrid Search --> ES
        ES -- 4. Top K Results --> Reranker[Reranker (重排序)]
        Reranker -- 5. Final Results --> User
    end

    subgraph AI / RAG Pipeline
        User -- "@AI 帮我总结周报" --> Agent[AI Agent Controller]
        Agent -- 1. 检索上下文 --> QueryBuilder
        Reranker -- 2. Context --> LLM[LLM (DeepSeek/GPT)]
        LLM -- 3. Answer --> User
    end
```

---

### 4. 核心索引结构设计 (Index Mapping)
我们需要针对不同类型的数据建立不同的索引策略。

#### 4.1 IM 消息索引 (`idx_im_messages`)
IM 消息短小、高频、权限依赖于会话。

```json
{
  "mappings": {
    "properties": {
      "msg_id": { "type": "keyword" },
      "channel_id": { "type": "long" }, 
      "sender_id": { "type": "long" },
      "content": { 
        "type": "text", 
        "analyzer": "ik_max_word", // 中文分词
        "copy_to": "all_text"
      },
      "vector": { // 向量字段
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine"
      },
      "created_at": { "type": "date" }
    }
  }
}
```

#### 4.2 文档索引 (`idx_documents`)
文档内容长，必须进行 **Chunking (切片)**。一个 50 页的 PDF 可能被切成 100 个 Document 存入 ES，但它们共享同一个 `doc_id`。

```json
{
  "mappings": {
    "properties": {
      "chunk_id": { "type": "keyword" },
      "parent_doc_id": { "type": "long" }, // 关联原始文档ID
      "title": { "type": "text", "analyzer": "ik_smart" },
      "content_chunk": { "type": "text", "analyzer": "ik_max_word" },
      "vector": { "type": "dense_vector", "dims": 1536 },
      
      // 权限字段 (关键)
      "allow_users": { "type": "long" },   // 白名单用户ID
      "allow_depts": { "type": "long" },   // 白名单部门ID
      "is_public": { "type": "boolean" }
    }
  }
}
```

---

### 5. 核心功能模块详细设计
#### 5.1 数据摄入与处理 (Ingestion Pipeline)
这是“写”的入口。

+ **输入**: RocketMQ `IM_MSG_SEND` (IM消息), `DOC_UPDATE` (文档更新)。
+ **文档解析 (Tika)**:
    - 如果是文件消息（如 PDF），`search-service` 从 MinIO 下载文件流，送入 Apache Tika 提取纯文本。
+ **智能切片 (Smart Chunking)**:
    - 不能简单按字符数切分，会截断语义。
    - **策略**: 使用 `RecursiveCharacterTextSplitter`，优先按段落 (`\n\n`) 切分，其次按句子 (`。`) 切分。每个 Chunk 约 500 Tokens，且保留 10% 的重叠 (Overlap)。
+ **向量化**: 调用 Embedding API 将文本转为向量。

#### 5.2 权限感知搜索 (Security-Aware Search)
这是企业级搜索最容易被忽视的环节：**用户只能搜到他有权看到的东西。**

+ **策略**: **Pre-filtering (查询前过滤)**。
+ **逻辑**:
    1. 前端发起搜索请求 `query="项目预算"`, `uid=1001`。
    2. `search-service` 调用 `im-service` 或 Redis 获取用户加入的所有 `channel_ids`。
    3. 调用 `user-service` 获取用户的 `dept_path` 和 `role_ids`。
    4. **构建 ES Query**:

```json
{
  "bool": {
    "must": [
      { "match": { "content": "项目预算" } } // 关键词匹配
    ],
    "filter": [
      {
        "bool": {
          "should": [
            { "terms": { "channel_id": [100, 102, 999] } }, // 只能搜我加入的群
            { "terms": { "allow_users": [1001] } },         // 文档权限
            { "terms": { "allow_depts": [10, 50] } },       // 部门权限
            { "term":  { "is_public": true } }
          ]
        }
      }
    ]
  }
}
```

+ **优势**: 在底层 Lucene 扫描倒排索引之前就过滤掉了无权文档，性能极高且绝对安全。

#### 5.3 RAG 问答 (Ask AI)
实现“与数据对话”。

+ **流程**:
    1. **Rewrite**: 将用户的问题“它去年的营收是多少？”改写为独立问题“飞书科技2024年的营收是多少？”（利用 LLM 历史对话上下文）。
    2. **Retrieve**: 在 ES 中进行混合检索（Vector + Keyword），召回 Top 10 相关片段。
    3. **Rerank (重排序)**: 使用 `bge-reranker` 模型对 Top 10 进行精细打分，取 Top 3。
    4. **Generate**: 构建 Prompt：

```latex
你是一个企业助手。请基于以下参考信息回答用户问题。如果信息不足，请说不知道。

参考信息:
[1] 2024财报.pdf: 营收为50亿...
[2] 总经理会议纪要: 去年营收增长...

用户问题: 飞书科技2024年的营收是多少？
```

    5. **Stream**: 将 LLM 的回答流式推送到前端。

#### 5.4 MCP Server 实现
为了让未来的 AI Agent（如 Claude Desktop 或 Cursor）能直接调用你的系统数据，需要实现 **Model Context Protocol (MCP)**。

+ **工具暴露**:
    - `search_messages(query, sender_id, time_range)`
    - `read_document(doc_id)`
    - `get_user_schedule(user_id)`
+ **实现方式**: 使用 Java 的 MCP SDK（或者简单的 JSON-RPC over SSE）。当 AI 决定调用 `search_messages` 时，`search-service` 执行 ES 查询并返回 JSON 结果，AI 再根据结果生成自然语言回答。

---

### 6. 关键代码实现 (Java)
#### 6.1 数据摄入消费者 (`IngestListener.java`)
```java
@Component
@RocketMQMessageListener(topic = "IM_MSG_SEND", consumerGroup = "search-ingest-group")
public class IngestListener implements RocketMQListener<MsgEvent> {

    @Autowired
    private ElasticsearchClient esClient;
    @Autowired
    private EmbeddingModel embeddingModel; // LangChain4j

    @Override
    public void onMessage(MsgEvent event) {
        // 1. 文本处理
        String text = event.getContent();
        
        // 2. 向量化 (调用 OpenAI 或 本地模型)
        Response<Embedding> response = embeddingModel.embed(text);
        List<Float> vector = response.content().vectorAsList();

        // 3. 构建 ES 文档
        ImMessageDoc doc = new ImMessageDoc();
        doc.setMsgId(event.getMsgId());
        doc.setChannelId(event.getChannelId());
        doc.setContent(text);
        doc.setVector(vector);
        doc.setCreatedAt(event.getTimestamp());

        // 4. 写入 ES
        esClient.index(i -> i.index("idx_im_messages").id(doc.getMsgId()).document(doc));
    }
}
```

#### 6.2 混合检索服务 (`SearchService.java`)
```java
@Service
public class SearchService {

    @Autowired
    private ElasticsearchClient esClient;
    @Autowired
    private EmbeddingModel embeddingModel;

    public SearchResult search(String query, Long userId, List<Long> channelIds) {
        // 1. 生成查询向量
        List<Float> queryVector = embeddingModel.embed(query).content().vectorAsList();

        // 2. 构建混合查询 (KNN + Filter)
        SearchResponse<ImMessageDoc> response = esClient.search(s -> s
            .index("idx_im_messages")
            .query(q -> q.bool(b -> b
                .must(m -> m.match(t -> t.field("content").query(query))) // 关键词
                .filter(f -> f.terms(t -> t.field("channel_id").value(channelIds))) // 权限过滤
            ))
            .knn(k -> k
                .field("vector")
                .k(10)
                .numCandidates(100)
                .queryVector(queryVector) // 向量检索
                .filter(f -> f.terms(t -> t.field("channel_id").value(channelIds))) // 同样的权限过滤
            ),
            ImMessageDoc.class
        );
        
        return convert(response);
    }
}
```

---

### 7. 性能优化 (Performance Tuning)
1. **写入优化**:
    - **Bulk Processing**: 不要一条条写 ES，消费者端每 100 条或每 500ms 聚合一次 Bulk 写入。
    - **Refresh Interval**: 将 ES 的刷新间隔设为 `30s`（默认1s），牺牲准实时性换取写入吞吐量。
2. **查询优化**:
    - **Filter Caching**: ES 会自动缓存 Filter 上下文的结果。因为用户的权限（channel_ids）短期内不会变，这能极大加速查询。
    - **Force Merge**: 每天凌晨对旧索引执行 `force_merge`，减少 Segment 数量。
3. **向量优化**:
    - 使用 **HNSW** 算法索引向量，查询速度比暴力的 `script_score` 快几个数量级。
    - 量化 (Quantization): 如果内存紧张，可以使用 `int8` 量化向量，空间减少 4 倍，精度损失很小。

### 总结
`search-service` 不仅仅是“查库”，它是**结构化数据（Postgres）与非结构化数据（AI）的桥梁**。

通过 **Tika 解析 + Embedding 向量化 + Elasticsearch 混合检索 + Pre-filtering 权限控制**，你构建了一个既安全又智能的企业知识库。这是你的毕设/项目中最能体现“智能化”和“技术深度”的亮点模块。

