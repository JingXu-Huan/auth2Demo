package org.example.imgroupserver.mapper;

import com.example.domain.model.DepartmentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 部门节点 Mapper
 */
@Repository
public interface DepartmentNodeMapper extends Neo4jRepository<DepartmentNode, Long> {

    Optional<DepartmentNode> findByDeptId(String deptId);

    @Query("MATCH (d:Department) WHERE coalesce(d.deleted, false) = false RETURN d")
    List<DepartmentNode> findAllActive();

    @Query("MATCH (d:Department {orgId: $orgId}) WHERE coalesce(d.deleted, false) = false RETURN d")
    List<DepartmentNode> findByOrgIdActive(@Param("orgId") String orgId);

    @Query("MATCH (d:Department {parentDeptId: $parentDeptId}) WHERE coalesce(d.deleted, false) = false RETURN d")
    List<DepartmentNode> findByParentDeptId(@Param("parentDeptId") String parentDeptId);

    @Query("MATCH (u:User)-[r:BELONGS_TO]->(d:Department {deptId: $deptId}) RETURN count(u)")
    Long countMembers(@Param("deptId") String deptId);

    @Query("MATCH (u:User {userId: $userId}), (d:Department {deptId: $deptId}) " +
           "MERGE (u)-[r:BELONGS_TO]->(d) " +
           "SET r.primaryDepartment = $primaryDepartment, " +
           "    r.title = $title, " +
           "    r.sortOrder = coalesce(r.sortOrder, $sortOrder), " +
           "    r.joinedAt = coalesce(r.joinedAt, localdatetime())")
    void addOrUpdateMember(@Param("deptId") String deptId,
                           @Param("userId") Long userId,
                           @Param("primaryDepartment") Boolean primaryDepartment,
                           @Param("title") String title,
                           @Param("sortOrder") Integer sortOrder);

    @Query("MATCH (u:User {userId: $userId})-[r:BELONGS_TO]->(d:Department {deptId: $deptId}) DELETE r")
    void removeMember(@Param("deptId") String deptId, @Param("userId") Long userId);

    @Query("MATCH (u:User)-[r:BELONGS_TO]->(d:Department {deptId: $deptId}) " +
           "RETURN collect({" +
           "  userId: u.userId, " +
           "  name: coalesce(u.nickname, ''), " +
           "  avatar: u.avatar, " +
           "  department: d.name, " +
           "  title: r.title, " +
           "  primaryDepartment: coalesce(r.primaryDepartment, false), " +
           "  sortOrder: coalesce(r.sortOrder, 0), " +
           "  joinedAt: toString(r.joinedAt)" +
           "}) as members")
    List<Object> findDepartmentMembers(@Param("deptId") String deptId);

    @Query("MATCH (u:User)-[r:BELONGS_TO]->(d:Department {deptId: $deptId}) DELETE r")
    void removeAllMembers(@Param("deptId") String deptId);

    // 部门归属租户/组织：Department-[:OWNED_BY]->Organization
    @Query("MATCH (d:Department {deptId: $deptId}), (o:Organization {orgId: $orgId}) " +
           "MERGE (d)-[:OWNED_BY]->(o)")
    void bindDepartmentToOrganization(@Param("orgId") String orgId, @Param("deptId") String deptId);

    // 部门层级关系：父部门-[:PARENT_OF]->子部门
    @Query("MATCH (p:Department {deptId: $parentDeptId}), (c:Department {deptId: $deptId}) " +
           "MERGE (p)-[:PARENT_OF]->(c)")
    void bindSubDepartment(@Param("parentDeptId") String parentDeptId, @Param("deptId") String deptId);

    @Query("MATCH (p:Department)-[r:PARENT_OF]->(c:Department {deptId: $deptId}) DELETE r")
    void clearParentRelation(@Param("deptId") String deptId);

    // 部门负责人关系：User-[:MANAGES]->Department
    @Query("MATCH (u:User)-[r:MANAGES]->(d:Department {deptId: $deptId}) DELETE r")
    void clearDepartmentManager(@Param("deptId") String deptId);

    @Query("MATCH (u:User {userId: $userId}), (d:Department {deptId: $deptId}) " +
           "MERGE (u)-[:MANAGES]->(d)")
    void bindDepartmentManager(@Param("deptId") String deptId, @Param("userId") Long userId);
}
