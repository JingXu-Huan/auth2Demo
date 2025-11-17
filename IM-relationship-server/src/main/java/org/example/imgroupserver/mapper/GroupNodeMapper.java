package org.example.imgroupserver.mapper;

import com.example.domain.model.GroupNode;
import com.example.domain.model.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface GroupNodeMapper extends Neo4jRepository<GroupNode, Long> {
    
    Optional<GroupNode> findByGroupId(String groupId);
    
    /**
     * 获取群组所有成员的 userId 列表
     */
    @Query("MATCH (u:User)-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "RETURN u.userId")
    List<Long> findMemberUserIds(@Param("groupId") String groupId);
    
    /**
     * 获取群组的所有成员（包含角色信息）
     * 使用 collect 返回单个值避免映射错误
     */
    @Query("MATCH (u:User)-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "RETURN collect({" +
           "  userId: u.userId, " +
           "  nickname: u.nickname, " +
           "  avatar: u.avatar, " +
           "  status: u.status, " +
           "  role: r.role, " +
           "  joinedAt: toString(r.joinedAt), " +
           "  muted: COALESCE(r.muted, false), " +
           "  groupNickname: r.groupNickname" +
           "}) as members")
    List<Object> findMembersWithRoleByGroupId(@Param("groupId") String groupId);
    
    /**
     * 获取群组成员数量
     */
    @Query("MATCH (u:User)-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) RETURN count(u)")
    Long countMembers(@Param("groupId") String groupId);
    
    /**
     * 获取用户加入的所有群组
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group) " +
           "RETURN g, r ORDER BY r.joinedAt DESC SKIP $skip LIMIT $limit")
    List<GroupNode> findGroupsByUserId(@Param("userId") Long userId, @Param("skip") int skip, @Param("limit") int limit);
    
    /**
     * 统计用户加入的群组数量
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group) RETURN count(g)")
    Long countUserGroups(@Param("userId") Long userId);
    
    /**
     * 添加用户到群组
     */
    @Query("MATCH (u:User {userId: $userId}), (g:Group {groupId: $groupId}) " +
           "CREATE (u)-[r:MEMBER_OF {role: $role, joinedAt: localdatetime(), muted: false}]->(g)")
    void addMember(@Param("groupId") String groupId, @Param("userId") Long userId, @Param("role") String role);
    
    /**
     * 移除群成员
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) DELETE r")
    void removeMember(@Param("groupId") String groupId, @Param("userId") Long userId);
    
    /**
     * 更新成员角色
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "SET r.role = $role")
    void updateMemberRole(@Param("groupId") String groupId, @Param("userId") Long userId, @Param("role") String role);
    
    /**
     * 检查用户是否在群组中
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "RETURN count(r) > 0")
    Boolean isMember(@Param("groupId") String groupId, @Param("userId") Long userId);
    
    /**
     * 获取用户在群组中的角色
     */
    @Query("MATCH (u:User {userId: $userId})-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "RETURN r.role")
    String getMemberRole(@Param("groupId") String groupId, @Param("userId") Long userId);
    
    /**
     * 搜索群组（按名称）
     */
    @Query("MATCH (g:Group) WHERE g.name CONTAINS $keyword RETURN g LIMIT $limit")
    List<GroupNode> searchByName(@Param("keyword") String keyword, @Param("limit") int limit);
    
    /**
     * 删除群组及其所有关系
     */
    @Query("MATCH (g:Group {groupId: $groupId}) DETACH DELETE g")
    void deleteGroupAndRelationships(@Param("groupId") String groupId);
    
    /**
     * 获取群组成员（分页，按角色过滤）
     */
    @Query("MATCH (u:User)-[r:MEMBER_OF]->(g:Group {groupId: $groupId}) " +
           "WHERE $role IS NULL OR r.role = $role " +
           "RETURN u, r ORDER BY r.joinedAt SKIP $skip LIMIT $limit")
    List<Map<String, Object>> findMembersWithRole(@Param("groupId") String groupId, @Param("role") String role, @Param("skip") int skip, @Param("limit") int limit);
}
