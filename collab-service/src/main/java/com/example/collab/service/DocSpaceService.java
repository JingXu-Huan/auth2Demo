package com.example.collab.service;

import com.example.collab.entity.DocSpace;
import com.example.collab.mapper.DocSpaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档空间服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocSpaceService {

    private final DocSpaceMapper docSpaceMapper;

    /**
     * 创建空间
     */
    @Transactional
    public DocSpace createSpace(Long ownerId, String name, String description) {
        DocSpace space = new DocSpace()
                .setOwnerId(ownerId)
                .setName(name)
                .setDescription(description)
                .setIsPublic(false)
                .setAllowGuest(false)
                .setDocCount(0)
                .setMemberCount(1)
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        
        docSpaceMapper.insert(space);
        log.info("创建文档空间: spaceId={}, name={}", space.getId(), name);
        return space;
    }

    /**
     * 获取空间详情
     */
    public DocSpace getSpace(Long spaceId) {
        return docSpaceMapper.selectById(spaceId);
    }

    /**
     * 获取用户的空间列表
     */
    public List<DocSpace> getUserSpaces(Long userId) {
        return docSpaceMapper.findByOwner(userId);
    }

    /**
     * 获取团队的空间列表
     */
    public List<DocSpace> getTeamSpaces(Long teamId) {
        return docSpaceMapper.findByTeam(teamId);
    }

    /**
     * 更新空间
     */
    @Transactional
    public void updateSpace(Long spaceId, String name, String description, Boolean isPublic) {
        DocSpace space = docSpaceMapper.selectById(spaceId);
        if (space == null) {
            throw new RuntimeException("空间不存在");
        }
        
        if (name != null) space.setName(name);
        if (description != null) space.setDescription(description);
        if (isPublic != null) space.setIsPublic(isPublic);
        space.setUpdatedAt(LocalDateTime.now());
        
        docSpaceMapper.updateById(space);
        log.info("更新文档空间: spaceId={}", spaceId);
    }

    /**
     * 删除空间
     */
    @Transactional
    public void deleteSpace(Long spaceId, Long operatorId) {
        DocSpace space = docSpaceMapper.selectById(spaceId);
        if (space == null) {
            return;
        }
        if (!space.getOwnerId().equals(operatorId)) {
            throw new RuntimeException("无权限删除此空间");
        }
        
        docSpaceMapper.deleteById(spaceId);
        log.info("删除文档空间: spaceId={}", spaceId);
    }
}
