package org.example.imgroupserver.mapper;

import com.example.domain.model.ConversationNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationNodeMapper extends Neo4jRepository<ConversationNode, Long> {

    Optional<ConversationNode> findByConversationId(String conversationId);

    /**
     * 将用户加入会话（IN_CHAT 关系）
     */
    @Query("MATCH (u:User {userId: $userId}), (c:Conversation {conversationId: $conversationId}) " +
           "MERGE (u)-[:IN_CHAT]->(c)")
    void addMember(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    /**
     * 查询会话的所有参与用户ID
     */
    @Query("MATCH (u:User)-[:IN_CHAT]->(c:Conversation {conversationId: $conversationId}) " +
           "RETURN u.userId")
    List<Long> findMemberUserIds(@Param("conversationId") String conversationId);
}
