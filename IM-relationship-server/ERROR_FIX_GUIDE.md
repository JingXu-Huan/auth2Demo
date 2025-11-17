# IM-group-server 错误修复指南

## ❌ 当前错误

### 错误 1: DataSource 配置失败
```
Failed to configure a DataSource: 'url' attribute is not specified
```

**原因：** Spring Boot 自动配置仍在尝试配置 JDBC DataSource，但我们已经改用 Neo4j

**✅ 已修复：** 在 `ImGroupServerApplication.java` 中排除了 `DataSourceAutoConfiguration`

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

---

### 错误 2: 旧的 MyBatis Mapper 仍然存在

**问题文件：**
- ❌ `GroupMapper.java` - 旧的 MyBatis Mapper
- ❌ `GroupMemberMapper.java` - 旧的 MyBatis Mapper

**✅ 解决方案：** 运行 `rename_old_mappers.bat` 脚本重命名这些文件

---

### 错误 3: GroupService 引用错误

`GroupService.java` 仍然引用旧的实体和 Mapper

**需要修复的导入：**
```java
// ❌ 错误的导入
import org.example.imgroupserver.mapper.GroupMapper;
import org.example.imgroupserver.mapper.GroupMemberMapper;

// ✅ 正确的导入
import org.example.imgroupserver.mapper.GroupNodeMapper;
import com.example.domain.model.GroupNode;
```

---

## 🔧 立即执行的修复步骤

### 步骤 1: 运行重命名脚本

```bash
cd G:\Projects\Java_Study\test\01\auth2Demo\IM-group-server
rename_old_mappers.bat
```

这会将旧的 Mapper 重命名为 `.bak` 文件。

---

### 步骤 2: 重命名旧的 GroupService

```bash
cd src\main\java\org\example\imgroupserver\service
ren GroupService.java GroupService.java.bak
```

---

### 步骤 3: 创建新的 Neo4jGroupService

创建文件：`src/main/java/org/example/imgroupserver/service/Neo4jGroupService.java`

```java
package org.example.imgroupserver.service;

import com.example.domain.dto.AddMembersRequest;
import com.example.domain.dto.CreateGroupRequest;
import com.example.domain.dto.UpdateGroupRequest;
import com.example.domain.model.GroupNode;
import com.example.domain.model.UserNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.mapper.GroupNodeMapper;
import org.example.imgroupserver.mapper.UserNodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jGroupService {
    
    private final GroupNodeMapper groupMapper;
    private final UserNodeMapper userMapper;
    
    /**
     * 创建群组
     */
    @Transactional
    public GroupNode createGroup(CreateGroupRequest request) {
        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId 不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("群组名称不能为空");
        }

        String groupId = generateGroupId();

        GroupNode group = new GroupNode();
        group.setGroupId(groupId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setAvatar(request.getAvatar());
        group.setOwnerId(request.getOwnerId());
        group.setMaxMembers(request.getMaxMembers() != null ? request.getMaxMembers() : 500);
        group.setJoinType(request.getJoinType() != null ? request.getJoinType().name() : "FREE");
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());

        GroupNode savedGroup = groupMapper.save(group);

        // 确保群主用户节点存在
        ensureUserNodeExists(request.getOwnerId());

        // 添加群主关系
        groupMapper.addMember(savedGroup.getGroupId(), request.getOwnerId(), "OWNER");

        // 添加初始成员
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<Long> distinctIds = request.getMemberIds().stream()
                    .filter(id -> !Objects.equals(id, request.getOwnerId()))
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());
            for (Long userId : distinctIds) {
                ensureUserNodeExists(userId);
                groupMapper.addMember(savedGroup.getGroupId(), userId, "MEMBER");
            }
        }

        return savedGroup;
    }

    /**
     * 获取群组详情
     */
    public GroupNode getGroup(String groupId) {
        return groupMapper.findByGroupId(groupId)
                .orElseThrow(() -> new NoSuchElementException("群组不存在: " + groupId));
    }

    /**
     * 更新群组基础信息
     */
    public GroupNode updateGroup(String groupId, UpdateGroupRequest request) {
        GroupNode group = getGroup(groupId);
        if (request.getName() != null) {
            group.setName(request.getName());
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription());
        }
        if (request.getAvatar() != null) {
            group.setAvatar(request.getAvatar());
        }
        if (request.getAnnouncement() != null) {
            group.setAnnouncement(request.getAnnouncement());
        }
        if (request.getJoinType() != null) {
            group.setJoinType(request.getJoinType().name());
        }
        group.setUpdatedAt(LocalDateTime.now());
        return groupMapper.save(group);
    }

    /**
     * 解散群组
     */
    @Transactional
    public void dissolveGroup(String groupId) {
        if (groupMapper.findByGroupId(groupId).isEmpty()) {
            throw new NoSuchElementException("群组不存在: " + groupId);
        }
        groupMapper.deleteGroupAndRelationships(groupId);
    }

    /**
     * 添加成员到群组
     */
    @Transactional
    public Map<String, Object> addMembers(String groupId, AddMembersRequest request) {
        GroupNode group = getGroup(groupId);
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new IllegalArgumentException("userIds 不能为空");
        }

        List<Long> failedUsers = new ArrayList<>();
        int addedCount = 0;

        for (Long userId : request.getUserIds()) {
            // 检查是否已在群中
            if (groupMapper.isMember(groupId, userId)) {
                failedUsers.add(userId);
                continue;
            }
            
            // 检查群组人数限制
            Long memberCount = groupMapper.countMembers(groupId);
            if (memberCount >= group.getMaxMembers()) {
                failedUsers.add(userId);
                continue;
            }
            
            ensureUserNodeExists(userId);
            groupMapper.addMember(groupId, userId, "MEMBER");
            addedCount++;
        }

        Long currentMemberCount = groupMapper.countMembers(groupId);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("addedCount", addedCount);
        result.put("failedUsers", failedUsers);
        result.put("currentMemberCount", currentMemberCount);
        return result;
    }

    /**
     * 移除群成员
     */
    @Transactional
    public Map<String, Object> removeMember(String groupId, Long userId) {
        getGroup(groupId);
        String role = groupMapper.getMemberRole(groupId, userId);
        if (role == null) {
            throw new NoSuchElementException("成员不存在于群组中");
        }
        if ("OWNER".equals(role)) {
            throw new IllegalStateException("不能直接移除群主，请先转让或解散群组");
        }

        groupMapper.removeMember(groupId, userId);
        Long count = groupMapper.countMembers(groupId);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("removedUserId", userId);
        result.put("currentMemberCount", count);
        return result;
    }

    /**
     * 获取群成员列表（分页）
     */
    public Map<String, Object> listMembers(String groupId, String role, int page, int size) {
        List<UserNode> allMembers = groupMapper.findMembersByGroupId(groupId);
        Long total = groupMapper.countMembers(groupId);

        // 手动分页
        int skip = (page - 1) * size;
        List<UserNode> pagedMembers = allMembers.stream()
                .skip(skip)
                .limit(size)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("members", pagedMembers);
        return result;
    }

    /**
     * 设置或取消群管理员
     */
    @Transactional
    public Map<String, Object> setAdmin(String groupId, Long userId, String action) {
        getGroup(groupId);
        String currentRole = groupMapper.getMemberRole(groupId, userId);
        if (currentRole == null) {
            throw new NoSuchElementException("成员不存在于群组中");
        }
        if ("OWNER".equals(currentRole)) {
            throw new IllegalStateException("群主角色无法更改");
        }
        
        String newRole;
        if ("ADD".equalsIgnoreCase(action)) {
            newRole = "ADMIN";
        } else if ("REMOVE".equalsIgnoreCase(action)) {
            newRole = "MEMBER";
        } else {
            throw new IllegalArgumentException("action 只能为 ADD 或 REMOVE");
        }
        
        groupMapper.updateMemberRole(groupId, userId, newRole);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("userId", userId);
        result.put("role", newRole);
        return result;
    }

    /**
     * 用户主动退出群组
     */
    @Transactional
    public Map<String, Object> leaveGroup(String groupId, Long userId) {
        getGroup(groupId);
        String role = groupMapper.getMemberRole(groupId, userId);
        if (role == null) {
            throw new NoSuchElementException("用户不在群组中");
        }
        if ("OWNER".equals(role)) {
            throw new IllegalStateException("群主不能直接退群，请先转让群主或解散群组");
        }

        groupMapper.removeMember(groupId, userId);
        Long count = groupMapper.countMembers(groupId);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("leftUserId", userId);
        result.put("currentMemberCount", count);
        return result;
    }

    /**
     * 搜索群成员
     */
    public List<UserNode> searchMembers(String groupId, String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        // 简化实现：获取所有成员后过滤
        List<UserNode> allMembers = groupMapper.findMembersByGroupId(groupId);
        return allMembers.stream()
                .filter(member -> member.getNickname() != null && 
                        member.getNickname().contains(keyword.trim()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取用户加入的群组列表
     */
    public Map<String, Object> getUserGroups(Long userId, int page, int size) {
        int skip = (page - 1) * size;
        List<GroupNode> groups = groupMapper.findGroupsByUserId(userId, skip, size);
        Long total = groupMapper.countUserGroups(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("groups", groups);
        return result;
    }

    /**
     * 转让群主
     */
    @Transactional
    public Map<String, Object> transferOwnership(String groupId, Long currentOwnerId, Long newOwnerId) {
        GroupNode group = getGroup(groupId);

        // 验证当前群主
        if (!Objects.equals(group.getOwnerId(), currentOwnerId)) {
            throw new IllegalArgumentException("当前用户不是群主");
        }

        // 验证新群主是否在群中
        if (!groupMapper.isMember(groupId, newOwnerId)) {
            throw new NoSuchElementException("新群主不在群组中");
        }

        // 更新角色
        groupMapper.updateMemberRole(groupId, currentOwnerId, "MEMBER");
        groupMapper.updateMemberRole(groupId, newOwnerId, "OWNER");

        // 更新群组拥有者
        group.setOwnerId(newOwnerId);
        group.setUpdatedAt(LocalDateTime.now());
        groupMapper.save(group);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("oldOwnerId", currentOwnerId);
        result.put("newOwnerId", newOwnerId);
        return result;
    }

    /**
     * 确保用户节点存在
     */
    private void ensureUserNodeExists(Long userId) {
        if (userMapper.findByUserId(userId).isEmpty()) {
            UserNode user = new UserNode();
            user.setUserId(userId);
            user.setNickname("用户" + userId);
            user.setStatus("ONLINE");
            userMapper.save(user);
        }
    }

    private String generateGroupId() {
        return "group_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
```

---

### 步骤 4: 更新 GroupController

修改 `GroupController.java`，将注入的 Service 改为 `Neo4jGroupService`：

```java
@RestController
@RequestMapping("/v1/groups")
@RequiredArgsConstructor
public class GroupController {
    
    // 改为注入 Neo4jGroupService
    private final Neo4jGroupService groupService;
    
    // 其他代码保持不变
}
```

---

### 步骤 5: 启动 Neo4j 数据库

```bash
docker run -d --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password123 \
  neo4j:latest
```

---

### 步骤 6: 更新 application.yml 密码

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: password123  # 改为实际密码
```

---

### 步骤 7: 重新编译

```bash
cd G:\Projects\Java_Study\test\01\auth2Demo
mvn clean install -DskipTests
```

---

## 📋 完整的错误清单

### ✅ 已修复
1. ✅ DataSource 自动配置已排除
2. ✅ Neo4j 实体已移到 domain 模块
3. ✅ Neo4j Mapper 已创建
4. ✅ 配置文件已更新

### ⚠️ 需要手动处理
1. ❌ 运行 `rename_old_mappers.bat` 重命名旧 Mapper
2. ❌ 重命名旧的 `GroupService.java`
3. ❌ 创建新的 `Neo4jGroupService.java`
4. ❌ 更新 `GroupController.java` 注入
5. ❌ 启动 Neo4j 数据库
6. ❌ 更新 `application.yml` 密码

---

## 🎯 预期结果

完成所有步骤后，应用应该能够：
1. ✅ 成功启动，不再报 DataSource 错误
2. ✅ 连接到 Neo4j 数据库
3. ✅ 使用 Neo4j 存储群组和好友关系
4. ✅ 所有 API 接口正常工作

---

## 🚨 如果仍有错误

请提供完整的错误日志，我会继续帮你修复！
