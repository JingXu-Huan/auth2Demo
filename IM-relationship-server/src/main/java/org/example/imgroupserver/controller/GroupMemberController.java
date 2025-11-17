package org.example.imgroupserver.controller;

import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.dto.GroupMemberDTO;
import org.example.imgroupserver.service.Neo4jGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GroupMemberController {

    private final Neo4jGroupService groupService;

    /**
     * 用户主动退出群组
     */
    @DeleteMapping("/{groupId}/members/leave")
    public ResponseEntity<Result<Map<String, Object>>> leaveGroup(@PathVariable("groupId") String groupId,
                                                                  @RequestParam("userId") Long userId) {
        Map<String, Object> data = groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok(Result.success("已退出群组", data));
    }

    /**
     * 搜索群成员
     */
    @GetMapping("/{groupId}/members/search")
    public ResponseEntity<Result<List<GroupMemberDTO>>> searchMembers(@PathVariable("groupId") String groupId,
                                                                       @RequestParam("keyword") String keyword,
                                                                       @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<GroupMemberDTO> members = groupService.searchMembers(groupId, keyword, limit);
        return ResponseEntity.ok(Result.success("搜索成功", members));
    }

    /**
     * 获取用户加入的群组列表
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Result<Map<String, Object>>> getUserGroups(@PathVariable("userId") Long userId,
                                                                     @RequestParam(value = "page", defaultValue = "1") int page,
                                                                     @RequestParam(value = "size", defaultValue = "20") int size) {
        Map<String, Object> data = groupService.getUserGroups(userId, page, size);
        return ResponseEntity.ok(Result.success("获取成功", data));
    }

    /**
     * 转让群主
     */
    @PostMapping("/{groupId}/transfer")
    public ResponseEntity<Result<Map<String, Object>>> transferOwnership(@PathVariable("groupId") String groupId,
                                                                         @RequestParam("currentOwnerId") Long currentOwnerId,
                                                                         @RequestParam("newOwnerId") Long newOwnerId) {
        Map<String, Object> data = groupService.transferOwnership(groupId, currentOwnerId, newOwnerId);
        return ResponseEntity.ok(Result.success("转让成功", data));
    }
}
