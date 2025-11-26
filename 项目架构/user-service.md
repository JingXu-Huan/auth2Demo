在“飞书/Lark 模式”下，组织架构不再是静态的层级，而是**高频变动的树状结构**。此服务的核心难点在于**如何高效处理树形数据的变更（特别是“移动部门”操作）**，以及如何保证 PostgreSQL（权威源）与 Neo4j（查询加速层）的数据一致性。

---

### 1. 核心定位与职责
+ **权威数据源 (Source of Truth)**：负责 `departments` 和 `dept_user_relation` 表的原子性写入。
+ **树形结构维护者**: 负责计算和更新物化路径 (`path`)，保证树结构的完整性。
+ **通讯录引擎**: 提供扁平化（搜索）和层级化（组织树）的通讯录查询能力。
+ **同步生产者**: 将组织变更事件发送至 MQ，驱动 Neo4j 图谱更新。

---

### 2. 核心 API 接口设计
#### 2.1 部门管理域 (Department)
| 方法 | 路径 | 描述 | 核心逻辑 |
| :--- | :--- | :--- | :--- |
| POST | `/api/org/depts` | 创建部门 | 计算 `path` = 父path + 父ID + "/" |
| PUT | `/api/org/depts/{id}` | 修改部门信息 | 仅修改名称/属性，不涉及结构 |
| PUT | `/api/org/depts/{id}/move` | **移动部门 (难点)** | 修改 `parent_id`，级联更新子孙节点的 `path` |
| DELETE | `/api/org/depts/{id}` | 删除部门 | 必须先校验该部门下无成员且无子部门 |
| GET | `/api/org/tree` | 获取组织架构树 | 支持懒加载 (`lazy=true`) 或全量加载 |


#### 2.2 人员管理域 (Member)
| 方法 | 路径 | 描述 | 核心逻辑 |
| :--- | :--- | :--- | :--- |
| POST | `/api/org/members` | 邀请/添加成员 | 关联 `users` 表，写入 `dept_user_relation` |
| PUT | `/api/org/members/{uid}/dept` | 调整成员部门 | 修改关联关系，处理 `is_primary` 逻辑 |
| GET | `/api/org/members/search` | 通讯录搜索 | 基于 ES 或 DB Like 查询 (昵称/拼音/邮箱) |
| GET | `/api/org/depts/{id}/members` | 获取部门成员 | 分页查询，支持递归 (`recursive=true`) |


---

### 3. 核心业务逻辑实现
#### 3.1 物化路径 (Materialized Path) 的计算策略
为了实现 O(1) 的子树查询，我们必须严格维护 `path` 字段。

+ **根节点**: `path = "/"`
+ **算法**: `CurrentPath = ParentPath + ParentID + "/"`

**示例**:

1. **总部 (ID:1)**: `path = "/"`
2. **研发部 (ID:100)** (Parent: 1): `path = "/" + "1" + "/" = "/1/"`
3. **后端组 (ID:999)** (Parent: 100): `path = "/1/" + "100" + "/" = "/1/100/"`

#### 3.2 移动部门 (Move Department) - 核心难点
当管理员将“后端组”从“研发部”移动到“架构部”下时，不仅要修改“后端组”的 `path`，还要级联修改“后端组”下面所有子小组的 `path`。

**算法流程**:

1. **锁定**: 开启事务，SELECT FOR UPDATE 锁定目标部门及其子树。
2. **计算前缀**:
    - 旧前缀 (`oldPrefix`): `/1/100/` (原父路径 + 原父ID + /)
    - 新前缀 (`newPrefix`): `/1/200/` (新父路径 + 新父ID + /)
3. **更新自身**: 修改当前部门的 `parent_id` 和 `path`。
4. **级联更新子孙**:

```sql
-- PostgreSQL 高效替换
UPDATE departments 
SET path = overlay(path placing :newPrefix from 1 for length(:oldPrefix))
WHERE path LIKE :oldPrefix || '%';
```

5. **发送事件**: 发送 `DEPT_MOVED` 事件到 MQ。

#### 3.3 成员的多部门归属 (Multi-Department Membership)
飞书模式允许一个用户属于多个部门，但必须有一个**主属部门 (Primary Department)**。

**逻辑**:

+ `dept_user_relation` 表联合主键 `(dept_id, user_id)`。
+ **设置主部门**:
    1. 开启事务。
    2. `UPDATE dept_user_relation SET is_primary = false WHERE user_id = ?` (先全设为否)。
    3. `UPDATE dept_user_relation SET is_primary = true WHERE user_id = ? AND dept_id = ?` (设目标为主)。
    4. 提交事务。

---

### 4. 关键代码实现 (Java)
#### 4.1 DepartmentService.java (移动部门)
```java
@Service
public class DepartmentService {

    @Autowired
    private DepartmentMapper deptMapper;
    @Autowired
    private RocketMQTemplate rocketMQ;

    @Transactional(rollbackFor = Exception.class)
    public void moveDepartment(Long deptId, Long newParentId) {
        // 1. 查询当前节点和新父节点
        Department dept = deptMapper.selectById(deptId);
        Department newParent = deptMapper.selectById(newParentId);
        
        // 2. 校验环路 (不能移动到自己的子节点下)
        if (newParent.getPath().startsWith(dept.getPath() + dept.getId() + "/")) {
            throw new BizException("Cannot move to its own descendant");
        }

        // 3. 计算路径变更
        String oldPathPrefix = dept.getPath() + dept.getId() + "/";
        String newPathPrefix = newParent.getPath() + newParent.getId() + "/";

        // 4. 更新自身
        dept.setParentId(newParentId);
        dept.setPath(newParent.getPath() + newParent.getId() + "/");
        deptMapper.updateById(dept);

        // 5. 批量更新子孙节点 (MyBatis Mapper)
        // SQL: UPDATE departments SET path = REPLACE(path, #{old}, #{new}) WHERE path LIKE #{old}%
        deptMapper.updateChildPaths(oldPathPrefix, newPathPrefix);

        // 6. 发送事件 (Sync to Neo4j)
        // 包含 oldParentId 和 newParentId，方便图数据库删除旧边、建立新边
        OrgEvent event = new OrgEvent("DEPT_MOVED", deptId, oldPathPrefix, newPathPrefix);
        rocketMQ.convertAndSend("ORG_EVENT", event);
    }
}
```

#### 4.2 递归获取子树 (Tree Query)
虽然前端可以处理树形渲染，但后端通常需要提供“整树”或“懒加载树”的 API。

```java
public List<DepartmentNode> getTree() {
    // 1. 全量查询 (组织架构通常数据量不大，<1万条可以直接全量查)
    List<Department> allDepts = deptMapper.selectList(null);
    
    // 2. 内存构建树 (Map法，O(N)复杂度)
    Map<Long, DepartmentNode> nodeMap = new HashMap<>();
    List<DepartmentNode> roots = new ArrayList<>();
    
    // 转换节点
    for (Department d : allDepts) {
        nodeMap.put(d.getId(), convert(d));
    }
    
    // 组装树
    for (Department d : allDepts) {
        DepartmentNode node = nodeMap.get(d.getId());
        if (d.getParentId() == 0) {
            roots.add(node);
        } else {
            DepartmentNode parent = nodeMap.get(d.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            }
        }
    }
    return roots; // 返回树根列表
}
```

---

### 5. 数据同步与一致性 (Sync to Neo4j)
`user-service` 是生产者，`sync-worker` 是消费者。

+ **Topic**: `ORG_EVENT`
+ **消息体定义**:

```json
{
  "event": "MEMBER_ADDED", // DEPT_CREATED, DEPT_MOVED, MEMBER_REMOVED
  "targetId": 1001,
  "parentId": 50,         // 部门ID
  "payload": {
      "name": "张三",
      "title": "工程师"
  },
  "timestamp": 1711223344
}
```

+ **Neo4j 侧的处理逻辑 (在 sync-worker 中实现)**:
    - **DEPT****_****CREATED**: `CREATE (d:Dept {id: $id, name: $name})`
    - **MEMBER****_****ADDED**:

```cypher
MATCH (u:User {id: $uid}), (d:Dept {id: $deptId})
MERGE (u)-[:BELONGS_TO {title: $title}]->(d)
```

    - **DEPT****_****MOVED**:

```cypher
MATCH (d:Dept {id: $deptId})
MATCH (d)-[r:SUB_DEPT_OF]->(oldParent)
DELETE r
WITH d
MATCH (newParent:Dept {id: $newParentId})
MERGE (d)-[:SUB_DEPT_OF]->(newParent)
```

---

### 6. 通讯录与搜索优化 (Search Optimization)
在高频搜索场景下（如 IM 搜索联系人），直接 `LIKE` 查询 Postgres 效率一般。

**架构优化**:

1. **Elasticsearch 接入**:
    - 使用 Postgres 的逻辑复制 (Logical Replication) 或 MQ 将 `users` 和 `departments` 同步到 ES。
    - 索引结构: `user_index { id, name, pinyin, email, dept_name_path }`。
2. **拼音搜索**:
    - 在 Java 层引入 `pinyin4j`，保存用户时生成 `name_pinyin` (zhangsan) 和 `name_initial` (zs) 存入 DB/ES。
    - 支持用户输入 "zs" 搜到 "张三"。

### 7. 缓存策略 (Cache Strategy)
组织架构是**读多写少**的典型场景。

1. **用户详情缓存**: `Redis String: user:info:{uid}` (TTL 1小时，Update时清除)。
2. **部门树缓存**: 由于树构建耗 CPU，可以缓存整个 Tree 的 JSON 结构到 Redis `org:tree`。
    - **失效策略**: 任何部门增删改移动，直接删除该 Key。下次读取时重新从 DB 构建。

### 总结
`user-service` 的设计不仅要满足基础的 CRUD，核心在于：

1. **利用物化路径**解决 SQL 树查询性能问题。
2. **利用事务和级联更新**解决树的重构（移动）问题。
3. **利用 MQ 事件驱动**保证与图数据库（权限引擎）的最终一致性。

这是支撑上层复杂的权限判断和 IM 组织架构选人的基石。

