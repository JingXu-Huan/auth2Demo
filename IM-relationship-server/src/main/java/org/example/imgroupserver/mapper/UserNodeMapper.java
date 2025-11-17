package org.example.imgroupserver.mapper;

import com.example.domain.model.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNodeMapper extends Neo4jRepository<UserNode, Long> {
    
    Optional<UserNode> findByUserId(Long userId);
    
    /**
     * 统计用户数量（不加载关系）
     */
    @Query("MATCH (u:User {userId: $userId}) RETURN count(u)")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的所有好友
     */
    @Query("MATCH (u:User {userId: $userId})-[r:FRIEND_OF]->(friend:User) RETURN friend, r")
    List<UserNode> findFriendsByUserId(@Param("userId") Long userId);
    
    /**
     * 检查两个用户是否是好友
     */
    @Query("MATCH (u1:User {userId: $userId1})-[r:FRIEND_OF]-(u2:User {userId: $userId2}) RETURN count(r) > 0")
    Boolean areFriends(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * 添加好友关系（双向）
     */
    @Query("MATCH (u1:User {userId: $userId1}), (u2:User {userId: $userId2}) " +
           "CREATE (u1)-[r1:FRIEND_OF {createdAt: localdatetime(), remark: $remark, source: $source}]->(u2) " +
           "CREATE (u2)-[r2:FRIEND_OF {createdAt: localdatetime(), remark: '', source: $source}]->(u1)")
    void createFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2, @Param("remark") String remark, @Param("source") String source);
    
    /**
     * 删除好友关系（双向）
     */
    @Query("MATCH (u1:User {userId: $userId1})-[r:FRIEND_OF]-(u2:User {userId: $userId2}) DELETE r")
    void deleteFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * 搜索用户（按昵称）
     */
    @Query("MATCH (u:User) WHERE u.nickname CONTAINS $keyword RETURN u LIMIT $limit")
    List<UserNode> searchByNickname(@Param("keyword") String keyword, @Param("limit") int limit);
}
