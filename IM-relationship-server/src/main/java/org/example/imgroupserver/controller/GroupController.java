package org.example.imgroupserver.controller;

import com.example.domain.vo.Result;
import com.example.domain.dto.AddMembersRequest;
import com.example.domain.dto.CreateGroupRequest;
import com.example.domain.dto.SetAdminRequest;
import com.example.domain.dto.UpdateGroupRequest;
import com.example.domain.model.GroupNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.service.Neo4jGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupController {

    private final Neo4jGroupService groupService;

    /**
     * 1. 创建群组
     *
     * <p>根据请求体中的信息创建新群组，并将 ownerId 设为群主，memberIds 作为初始成员加入。</p>
     * <p>权限预期：当前未做鉴权，实际使用中应限制为登录用户调用，ownerId 通常为当前用户ID。</p>
     *
     * @param request CreateGroupRequest，包含群名称、描述、头像、最大成员数、加入方式、初始成员、群主ID等
     * @return Result<GroupNode>，data 为新建的群组实体
     */
    @PostMapping
    public ResponseEntity<Result<GroupNode>> createGroup(@RequestBody CreateGroupRequest request) {
        GroupNode group = groupService.createGroup(request);
        return ResponseEntity.ok(Result.success("创建成功", group));
    }

    /**
     * 2. 获取群组信息
     *
     * <p>根据 groupId 返回群组的基础信息。</p>
     * <p>权限预期：群成员或有查看权限的用户。</p>
     *
     * @param groupId 群组ID
     * @return Result<GroupNode>，data 为群组实体
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<Result<GroupNode>> getGroup(@PathVariable("groupId") String groupId) {
        GroupNode group = groupService.getGroup(groupId);
        return ResponseEntity.ok(Result.success("success", group));
    }

    /**
     * 3. 更新群组信息
     *
     * <p>更新群名称、描述、头像、公告、加入方式等字段，仅更新请求体中非空字段。</p>
     * <p>权限预期：群主或管理员。</p>
     *
     * @param groupId 群组ID
     * @param request UpdateGroupRequest，包含要修改的群信息字段
     * @return Result<GroupNode>，data 为更新后的群组实体
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<Result<GroupNode>> updateGroup(@PathVariable("groupId") String groupId,
                                                     @RequestBody UpdateGroupRequest request) {
        GroupNode group = groupService.updateGroup(groupId, request);
        return ResponseEntity.ok(Result.success("更新成功", group));
    }

    /**
     * 4. 解散群组
     *
     * <p>删除群组及其所有成员记录。</p>
     * <p>权限预期：仅群主可调用。</p>
     *
     * @param groupId 群组ID
     * @return Result<Void>，data 为空
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Result<Void>> dissolveGroup(@PathVariable("groupId") String groupId) {
        groupService.dissolveGroup(groupId);
        return ResponseEntity.ok(Result.success("群组已解散", null));
    }

    /**
     * 5. 添加成员
     *
     * <p>批量将用户添加到群组，跳过已在群中的成员以及超过最大人数限制的情况。</p>
     * <p>权限预期：群主或管理员。</p>
     *
     * @param groupId 群组ID
     * @param request AddMembersRequest，包含 userIds（待添加成员）和 inviterId（邀请人）
     * @return Result<Map>，data 包含 groupId、addedCount、failedUsers、currentMemberCount
     */
    @PostMapping("/{groupId}/members")
    public ResponseEntity<Result<Map<String, Object>>> addMembers(@PathVariable("groupId") String groupId,
                                                                  @RequestBody AddMembersRequest request) {
        Map<String, Object> data = groupService.addMembers(groupId, request);
        return ResponseEntity.ok(Result.success("添加成功", data));
    }

    /**
     * 6. 移除成员
     *
     * <p>将指定用户从群组中移除，禁止移除群主。</p>
     * <p>权限预期：群主或管理员。</p>
     *
     * @param groupId 群组ID
     * @param userId  要移除的用户ID
     * @return Result<Map>，data 包含 groupId、removedUserId、currentMemberCount
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Result<Map<String, Object>>> removeMember(@PathVariable("groupId") String groupId,
                                                                    @PathVariable("userId") Long userId) {
        Map<String, Object> data = groupService.removeMember(groupId, userId);
        return ResponseEntity.ok(Result.success("已移除成员", data));
    }

    /**
     * 7. 获取成员列表
     *
     * <p>分页返回指定群组的成员列表，可按角色过滤（OWNER/ADMIN/MEMBER/ALL）。</p>
     *
     * @param groupId 群组ID
     * @param page    页码，从1开始
     * @param size    每页数量
     * @param role    角色过滤：ALL/OWNER/ADMIN/MEMBER
     * @return Result<Map>，data 包含 total、page、size、members 列表
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<Result<Map<String, Object>>> listMembers(@PathVariable("groupId") String groupId,
                                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                                   @RequestParam(value = "size", defaultValue = "20") int size,
                                                                   @RequestParam(value = "role", required = false, defaultValue = "ALL") String role) {
        String memberRole = null;
        if (!"ALL".equalsIgnoreCase(role)) {
            memberRole = role.toUpperCase();
        }
        Map<String, Object> data = groupService.listMembers(groupId, memberRole, page, size);
        return ResponseEntity.ok(Result.success("success", data));
    }

    /**
     * 8. 设置管理员
     *
     * <p>根据 action 将某成员设为管理员或取消管理员身份。</p>
     * <p>权限预期：仅群主可调用。</p>
     *
     * @param groupId 群组ID
     * @param request SetAdminRequest，包含 userId（目标用户）和 action（ADD/REMOVE）
     * @return Result<Map>，data 包含 groupId、userId、role
     */
    @PostMapping("/{groupId}/admins")
    public ResponseEntity<Result<Map<String, Object>>> setAdmin(@PathVariable("groupId") String groupId,
                                                                @RequestBody SetAdminRequest request) {
        Map<String, Object> data = groupService.setAdmin(groupId, request.getUserId(), request.getAction());
        return ResponseEntity.ok(Result.success("设置成功", data));
    }
}
