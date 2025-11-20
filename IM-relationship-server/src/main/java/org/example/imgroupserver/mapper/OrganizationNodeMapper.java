package org.example.imgroupserver.mapper;

import com.example.domain.model.OrganizationNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 组织节点 Mapper
 */
@Repository
public interface OrganizationNodeMapper extends Neo4jRepository<OrganizationNode, Long> {

    Optional<OrganizationNode> findByOrgId(String orgId);
}
