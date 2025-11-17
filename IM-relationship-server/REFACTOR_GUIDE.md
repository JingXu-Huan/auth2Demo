# IM-group-server Neo4j 重构指南

## ✅ 已完成的工作

### 1. 依赖更新
- ✅ 更新 `pom.xml`，添加 `spring-boot-starter-data-neo4j`
- ✅ 移除 PostgreSQL 和 MyBatis-Plus 依赖

### 2. 实体层
已创建 Neo4j 节点和关系实体：
- ✅ `UserNode` - 用户节点
- ✅ `GroupNode` - 群组节点
- ✅ `MemberOfRelationship` - 用户加入群组关系
- ✅ `FriendOfRelationship` - 好友关系

### 3. Repository 层
已创建 Neo4j Repository 接口：
- ✅ `UserNodeRepository` - 用户节点操作
- ✅ `GroupNodeRepository` - 群组节点操作

### 4. 配置文件
- ✅ 更新 `application.yml`，配置 Neo4j 连接

---

## 🔄 需要手动完成的步骤

### 步骤 1: 安装和启动 Neo4j

```bash
# 使用 Docker 启动 Neo4j
docker run -d \
  --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/your_password_here \
  neo4j:latest
```

访问 http://localhost:7474 验证安装。

### 步骤 2: 更新 application.yml 密码

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your_password_here  # 改为实际密码
```

### 步骤 3: 创建新的 GroupService (基于 Neo4j)

创建文件：`src/main/java/org/example/imgroupserver/service/Neo4jGroupService.java`

```java
package org.example.imgroupserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.entity.GroupNode;
import org.example.imgroupserver.entity.UserNode;
import org.example.imgroupserver.repository.GroupNodeRepository;
import org.example.imgroupserver.repository.UserNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jGroupService {
    
    private final GroupNodeRepository groupRepository;
    private final UserNodeRepository userRepository;
    
    /**
     * 创建群组
     */
    @Transactional
    public GroupNode createGroup(String name, String description, Long ownerId, List<Long> memberIds) {
        // 1. 创建群组节点
        GroupNode group = new GroupNode();
        group.setGroupId(generateGroupId());
        group.setName(name);
        group.setDescription(description);
        group.setOwnerId(ownerId);
        group.setMaxMembers(500);
        group.setJoinType("FREE");
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        
        GroupNode savedGroup = groupRepository.save(group);
        
        // 2. 确保群主用户节点存在
        ensureUserNodeExists(ownerId);
        
        // 3. 添加群主关系
        groupRepository.addMember(savedGroup.getGroupId(), ownerId, "OWNER");
        
        // 4. 添加初始成员
        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (!memberId.equals(ownerId)) {
                    ensureUserNodeExists(memberId);
                    groupRepository.addMember(savedGroup.getGroupId(), memberId, "MEMBER");
                }
            }
        }
        
        return savedGroup;
    }
    
    /**
     * 获取群组详情
     */
    public GroupNode getGroup(String groupId) {
        return groupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new NoSuchElementException("群组不存在: " + groupId));
    }
    
    /**
     * 添加成员到群组
     */
    @Transactional
    public void addMember(String groupId, Long userId) {
        ensureUserNodeExists(userId);
        
        // 检查是否已在群中
        if (groupRepository.isMember(groupId, userId)) {
            throw new IllegalStateException("用户已在群组中");
        }
        
        // 检查群组人数限制
        GroupNode group = getGroup(groupId);
        Long memberCount = groupRepository.countMembers(groupId);
        if (memberCount >= group.getMaxMembers()) {
            throw new IllegalStateException("群组已满");
        }
        
        groupRepository.addMember(groupId, userId, "MEMBER");
    }
    
    /**
     * 移除群成员
     */
    @Transactional
    public void removeMember(String groupId, Long userId) {
        String role = groupRepository.getMemberRole(groupId, userId);
        if ("OWNER".equals(role)) {
            throw new IllegalStateException("不能移除群主");
        }
        groupRepository.removeMember(groupId, userId);
    }
    
    /**
     * 获取用户加入的群组列表
     */
    public Map<String, Object> getUserGroups(Long userId, int page, int size) {
        int skip = (page - 1) * size;
        List<GroupNode> groups = groupRepository.findGroupsByUserId(userId, skip, size);
        Long total = groupRepository.countUserGroups(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("groups", groups);
        return result;
    }
    
    /**
     * 获取群成员列表
     */
    public Map<String, Object> getMembers(String groupId, int page, int size) {
        List<UserNode> members = groupRepository.findMembersByGroupId(groupId);
        Long total = groupRepository.countMembers(groupId);
        
        // 手动分页
        int skip = (page - 1) * size;
        List<UserNode> pagedMembers = members.stream()
                .skip(skip)
                .limit(size)
                .toList();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("members", pagedMembers);
        return result;
    }
    
    /**
     * 解散群组
     */
    @Transactional
    public void dissolveGroup(String groupId) {
        groupRepository.deleteGroupAndRelationships(groupId);
    }
    
    /**
     * 设置管理员
     */
    @Transactional
    public void setAdmin(String groupId, Long userId, String action) {
        String currentRole = groupRepository.getMemberRole(groupId, userId);
        if ("OWNER".equals(currentRole)) {
            throw new IllegalStateException("群主角色无法更改");
        }
        
        String newRole = "ADD".equalsIgnoreCase(action) ? "ADMIN" : "MEMBER";
        groupRepository.updateMemberRole(groupId, userId, newRole);
    }
    
    /**
     * 确保用户节点存在
     */
    private void ensureUserNodeExists(Long userId) {
        if (userRepository.findByUserId(userId).isEmpty()) {
            UserNode user = new UserNode();
            user.setUserId(userId);
            user.setNickname("用户" + userId);
            user.setStatus("ONLINE");
            userRepository.save(user);
        }
    }
    
    private String generateGroupId() {
        return "group_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
```

### 步骤 4: 创建 FriendService

创建文件：`src/main/java/org/example/imgroupserver/service/FriendService.java`

```java
package org.example.imgroupserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.entity.UserNode;
import org.example.imgroupserver.repository.UserNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    
    private final UserNodeRepository userRepository;
    
    /**
     * 添加好友
     */
    @Transactional
    public void addFriend(Long userId, Long friendId, String remark, String source) {
        // 检查是否已是好友
        if (userRepository.areFriends(userId, friendId)) {
            throw new IllegalStateException("已经是好友关系");
        }
        
        // 确保两个用户节点都存在
        ensureUserExists(userId);
        ensureUserExists(friendId);
        
        // 创建双向好友关系
        userRepository.createFriendship(userId, friendId, remark, source);
    }
    
    /**
     * 删除好友
     */
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        userRepository.deleteFriendship(userId, friendId);
    }
    
    /**
     * 获取好友列表
     */
    public List<UserNode> getFriends(Long userId) {
        return userRepository.findFriendsByUserId(userId);
    }
    
    /**
     * 检查是否是好友
     */
    public Boolean areFriends(Long userId1, Long userId2) {
        return userRepository.areFriends(userId1, userId2);
    }
    
    /**
     * 搜索用户
     */
    public List<UserNode> searchUsers(String keyword, int limit) {
        return userRepository.searchByNickname(keyword, limit);
    }
    
    private void ensureUserExists(Long userId) {
        if (userRepository.findByUserId(userId).isEmpty()) {
            UserNode user = new UserNode();
            user.setUserId(userId);
            user.setNickname("用户" + userId);
            user.setStatus("ONLINE");
            userRepository.save(user);
        }
    }
}
```

### 步骤 5: 创建 FriendController

创建文件：`src/main/java/org/example/imgroupserver/controller/FriendController.java`

```java
package org.example.imgroupserver.controller;

import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.entity.UserNode;
import org.example.imgroupserver.service.FriendService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/friends")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    
    /**
     * 添加好友
     */
    @PostMapping
    public Result<Void> addFriend(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long friendId = Long.valueOf(request.get("friendId").toString());
        String remark = (String) request.getOrDefault("remark", "");
        String source = (String) request.getOrDefault("source", "SEARCH");
        
        friendService.addFriend(userId, friendId, remark, source);
        return Result.success();
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{userId}/{friendId}")
    public Result<Void> deleteFriend(@PathVariable Long userId, @PathVariable Long friendId) {
        friendService.deleteFriend(userId, friendId);
        return Result.success();
    }
    
    /**
     * 获取好友列表
     */
    @GetMapping("/{userId}")
    public Result<List<UserNode>> getFriends(@PathVariable Long userId) {
        List<UserNode> friends = friendService.getFriends(userId);
        return Result.success(friends);
    }
    
    /**
     * 检查是否是好友
     */
    @GetMapping("/check")
    public Result<Boolean> areFriends(@RequestParam Long userId1, @RequestParam Long userId2) {
        Boolean areFriends = friendService.areFriends(userId1, userId2);
        return Result.success(areFriends);
    }
    
    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<UserNode>> searchUsers(@RequestParam String keyword, 
                                               @RequestParam(defaultValue = "20") int limit) {
        List<UserNode> users = friendService.searchUsers(keyword, limit);
        return Result.success(users);
    }
}
```

### 步骤 6: 更新 GroupController

修改 `GroupController.java`，注入 `Neo4jGroupService` 替代原来的 `GroupService`：

```java
@RequiredArgsConstructor
public class GroupController {
    
    private final Neo4jGroupService groupService;  // 改为 Neo4jGroupService
    
    // 其他代码保持不变，方法调用会自动适配
}
```

### 步骤 7: 删除旧的 Mapper 文件

删除以下文件（不再需要）：
- `GroupMapper.java`
- `GroupMemberMapper.java`
- 旧的 `GroupService.java`（或重命名为 `GroupService.java.bak`）

### 步骤 8: 启用 Neo4j Repository

在 `ImGroupServerApplication.java` 中添加注解：

```java
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "org.example.imgroupserver.repository")
public class ImGroupServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImGroupServerApplication.class, args);
    }
}
```

---

## 🎯 Neo4j 数据模型

### 节点类型
1. **User** - 用户节点
   - userId (业务ID)
   - nickname
   - avatar
   - status

2. **Group** - 群组节点
   - groupId (业务ID)
   - name
   - description
   - avatar
   - ownerId
   - maxMembers
   - joinType

### 关系类型
1. **MEMBER_OF** - 用户加入群组
   - role (OWNER/ADMIN/MEMBER)
   - joinedAt
   - nickname (群昵称)
   - muted

2. **FRIEND_OF** - 好友关系（双向）
   - createdAt
   - remark
   - source

---

## 📊 Cypher 查询示例

```cypher
// 创建用户
CREATE (u:User {userId: 10, nickname: '张三', status: 'ONLINE'})

// 创建群组
CREATE (g:Group {groupId: 'group_123', name: '技术交流群', ownerId: 10})

// 添加群成员
MATCH (u:User {userId: 10}), (g:Group {groupId: 'group_123'})
CREATE (u)-[:MEMBER_OF {role: 'OWNER', joinedAt: datetime()}]->(g)

// 添加好友
MATCH (u1:User {userId: 10}), (u2:User {userId: 20})
CREATE (u1)-[:FRIEND_OF {createdAt: datetime(), remark: '同事'}]->(u2)
CREATE (u2)-[:FRIEND_OF {createdAt: datetime()}]->(u1)

// 查询用户的所有群组
MATCH (u:User {userId: 10})-[r:MEMBER_OF]->(g:Group)
RETURN g, r.role

// 查询群组的所有成员
MATCH (u:User)-[r:MEMBER_OF]->(g:Group {groupId: 'group_123'})
RETURN u, r.role

// 查询用户的好友
MATCH (u:User {userId: 10})-[r:FRIEND_OF]->(friend:User)
RETURN friend, r.remark

// 查询共同好友
MATCH (u1:User {userId: 10})-[:FRIEND_OF]->(common:User)<-[:FRIEND_OF]-(u2:User {userId: 20})
RETURN common

// 查询共同群组
MATCH (u1:User {userId: 10})-[:MEMBER_OF]->(g:Group)<-[:MEMBER_OF]-(u2:User {userId: 20})
RETURN g
```

---

## ✅ 测试清单

- [ ] Neo4j 数据库已启动
- [ ] 配置文件密码已更新
- [ ] 创建群组功能正常
- [ ] 添加/移除群成员功能正常
- [ ] 获取群组列表功能正常
- [ ] 获取群成员列表功能正常
- [ ] 添加好友功能正常
- [ ] 删除好友功能正常
- [ ] 获取好友列表功能正常
- [ ] 搜索用户功能正常

---

## 🚀 优势

使用 Neo4j 后的优势：

1. **图关系查询更高效**：好友关系、群组关系查询性能大幅提升
2. **复杂关系查询简单**：共同好友、共同群组等查询一行 Cypher 搞定
3. **关系属性丰富**：可以在关系上存储更多元数据（如加入时间、角色等）
4. **扩展性强**：未来可以轻松添加更多关系类型（如关注、黑名单等）
5. **可视化友好**：Neo4j Browser 可以直观展示关系图谱

---

## 📝 注意事项

1. Neo4j 默认端口：
   - HTTP: 7474
   - Bolt: 7687

2. 首次启动需要修改默认密码

3. 生产环境建议使用 Neo4j Enterprise 版本

4. 定期备份数据：
   ```bash
   neo4j-admin dump --database=neo4j --to=/backup/neo4j.dump
   ```

5. 性能优化：为常用查询字段创建索引
   ```cypher
   CREATE INDEX FOR (u:User) ON (u.userId)
   CREATE INDEX FOR (g:Group) ON (g.groupId)
   ```
