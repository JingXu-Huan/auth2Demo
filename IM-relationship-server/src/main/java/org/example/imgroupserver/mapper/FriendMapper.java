package org.example.imgroupserver.mapper;

import com.example.domain.model.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 好友关系 Mapper
 */
@Repository
public interface FriendMapper extends Neo4jRepository<UserNode, Long> {
    
    /**
     * 发送好友请求
     */
    @Query("MATCH (u1:User {userId: $fromUserId}), (u2:User {userId: $toUserId}) " +
           "MERGE (u1)-[r:HAS_REQUESTED {message: $message, status: 'PENDING', createdAt: localdatetime()}]->(u2) " +
           "RETURN id(r)")
    Long sendFriendRequest(@Param("fromUserId") Long fromUserId, 
                          @Param("toUserId") Long toUserId,
                          @Param("message") String message);
    
    /**
     * 检查是否已发送好友请求
     */
    @Query("MATCH (u1:User {userId: $fromUserId})-[r:HAS_REQUESTED]->(u2:User {userId: $toUserId}) " +
           "RETURN count(r) > 0")
    Boolean hasRequested(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
    
    /**
     * 接受好友请求（删除请求关系，创建好友关系）
     */
    @Query("MATCH (u1:User {userId: $fromUserId})-[r:HAS_REQUESTED]->(u2:User {userId: $toUserId}) " +
           "DELETE r " +
           "WITH u1, u2 " +
           "MERGE (u1)-[:FRIENDS_WITH {createdAt: localdatetime()}]->(u2) " +
           "MERGE (u2)-[:FRIENDS_WITH {createdAt: localdatetime()}]->(u1)")
    void acceptFriendRequest(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
    
    /**
     * 拒绝好友请求（删除请求关系）
     */
    @Query("MATCH (u1:User {userId: $fromUserId})-[r:HAS_REQUESTED]->(u2:User {userId: $toUserId}) " +
           "DELETE r")
    void rejectFriendRequest(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);
    
    /**
     * 获取收到的好友请求列表
     */
    @Query("MATCH (from:User)-[r:HAS_REQUESTED]->(to:User {userId: $userId}) " +
           "WHERE r.status = 'PENDING' " +
           "RETURN collect({" +
           "  requestId: id(r), " +
           "  fromUserId: from.userId, " +
           "  fromNickname: from.nickname, " +
           "  fromAvatar: from.avatar, " +
           "  toUserId: to.userId, " +
           "  message: r.message, " +
           "  status: r.status, " +
           "  createdAt: toString(r.createdAt)" +
           "}) as requests")
    List<Object> getReceivedRequests(@Param("userId") Long userId);
    
    /**
     * 获取发送的好友请求列表
     */
    @Query("MATCH (from:User {userId: $userId})-[r:HAS_REQUESTED]->(to:User) " +
           "WHERE r.status = 'PENDING' " +
           "RETURN collect({" +
           "  requestId: id(r), " +
           "  fromUserId: from.userId, " +
           "  toUserId: to.userId, " +
           "  message: r.message, " +
           "  status: r.status, " +
           "  createdAt: toString(r.createdAt)" +
           "}) as requests")
    List<Object> getSentRequests(@Param("userId") Long userId);
    
    /**
     * 检查是否已经是好友
     */
    @Query("MATCH (u1:User {userId: $userId1})-[r:FRIENDS_WITH]->(u2:User {userId: $userId2}) " +
           "RETURN count(r) > 0")
    Boolean isFriend(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * 获取用户的所有好友
     */
    @Query("MATCH (u:User {userId: $userId})-[r:FRIENDS_WITH]->(friend:User) " +
           "RETURN collect({" +
           "  userId: friend.userId, " +
           "  nickname: friend.nickname, " +
           "  avatar: friend.avatar, " +
           "  status: friend.status, " +
           "  createdAt: toString(r.createdAt)" +
           "}) as friends")
    List<Object> findFriends(@Param("userId") Long userId);
    
    /**
     * 删除好友关系（双向）
     */
    @Query("MATCH (u1:User {userId: $userId1})-[r1:FRIENDS_WITH]->(u2:User {userId: $userId2}) " +
           "MATCH (u2)-[r2:FRIENDS_WITH]->(u1) " +
           "DELETE r1, r2")
    void deleteFriendship(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    /**
     * 搜索好友
     */
    @Query("MATCH (u:User {userId: $userId})-[r:FRIENDS_WITH]->(friend:User) " +
           "WHERE friend.nickname CONTAINS $keyword " +
           "RETURN collect({" +
           "  userId: friend.userId, " +
           "  nickname: friend.nickname, " +
           "  avatar: friend.avatar, " +
           "  status: friend.status, " +
           "  createdAt: toString(r.createdAt)" +
           "}) as friends " +
           "LIMIT $limit")
    List<Object> searchFriends(@Param("userId") Long userId, 
                              @Param("keyword") String keyword, 
                              @Param("limit") int limit);
}
