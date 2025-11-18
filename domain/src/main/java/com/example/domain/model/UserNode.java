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
    
    // 用户的好友关系
    @Relationship(type = "FRIEND_OF", direction = Relationship.Direction.OUTGOING)
    private Set<FriendOfRelationship> friends = new HashSet<>();

}
