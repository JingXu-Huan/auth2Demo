package com.example.collab.controller;

import com.example.collab.entity.DocSpace;
import com.example.collab.service.DocSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文档空间控制器
 */
@RestController
@RequestMapping("/api/collab/spaces")
@RequiredArgsConstructor
public class DocSpaceController {

    private final DocSpaceService docSpaceService;

    /**
     * 创建空间
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSpace(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, String> params) {
        String name = params.get("name");
        String description = params.get("description");
        
        DocSpace space = docSpaceService.createSpace(userId, name, description);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("spaceId", space.getId());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取空间详情
     */
    @GetMapping("/{spaceId}")
    public ResponseEntity<DocSpace> getSpace(@PathVariable Long spaceId) {
        DocSpace space = docSpaceService.getSpace(spaceId);
        if (space == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(space);
    }

    /**
     * 获取我的空间列表
     */
    @GetMapping("/my")
    public ResponseEntity<List<DocSpace>> getMySpaces(@RequestHeader("X-User-Id") Long userId) {
        List<DocSpace> spaces = docSpaceService.getUserSpaces(userId);
        return ResponseEntity.ok(spaces);
    }

    /**
     * 获取团队空间列表
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<DocSpace>> getTeamSpaces(@PathVariable Long teamId) {
        List<DocSpace> spaces = docSpaceService.getTeamSpaces(teamId);
        return ResponseEntity.ok(spaces);
    }

    /**
     * 更新空间
     */
    @PutMapping("/{spaceId}")
    public ResponseEntity<Map<String, Object>> updateSpace(
            @PathVariable Long spaceId,
            @RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        String description = (String) params.get("description");
        Boolean isPublic = (Boolean) params.get("isPublic");
        
        docSpaceService.updateSpace(spaceId, name, description, isPublic);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    /**
     * 删除空间
     */
    @DeleteMapping("/{spaceId}")
    public ResponseEntity<Map<String, Object>> deleteSpace(
            @PathVariable Long spaceId,
            @RequestHeader("X-User-Id") Long userId) {
        docSpaceService.deleteSpace(spaceId, userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }
}
