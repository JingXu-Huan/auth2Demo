package com.example.domain.model;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户节点
 */
@Node("User")
@Data
public class UserNode {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private Long userId;  // 业务ID
    private String nickname;
    private String avatar;
    private String status;  // ONLINE, OFFLINE, BUSY
    
    // 用户加入的群组
    @Relationship(type = "MEMBER_OF")
    private Set<MemberOfRelationship> groups = new HashSet<>();

    // 用户所属的部门（组织架构）
    @Relationship(type = "BELONGS_TO_DEPT")
    private Set<DepartmentMemberRelationship> departments = new HashSet<>();
    
    // 用户的好友关系
    @Relationship(type = "FRIEND_OF", direction = Relationship.Direction.OUTGOING)
    private Set<FriendOfRelationship> friends = new HashSet<>();

    // 用户参与的会话（单聊 / 群聊统一抽象）
    @Relationship(type = "IN_CHAT")
    private Set<InChatRelationship> conversations = new HashSet<>();

}
