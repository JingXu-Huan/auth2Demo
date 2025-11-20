package org.example.imgroupserver.controller;

import com.example.domain.dto.*;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.imgroupserver.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组织架构自测接口
 * 用于快速验证组织架构功能是否正常
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/organization/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrganizationTestController {

    private final OrganizationService organizationService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "organization-service");
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(Result.success("组织架构服务运行正常", data));
    }

    /**
     * 快速创建测试组织架构
     * 创建一个根部门和两个子部门，用于测试
     */
    @PostMapping("/init-test-data")
    public ResponseEntity<Result<Map<String, Object>>> initTestData(@RequestParam(required = false) Long userId) {
        try {
            Long testUserId = userId != null ? userId : 1L;
            
            // 创建根部门（组织）
            CreateDepartmentRequest rootReq = new CreateDepartmentRequest();
            rootReq.setOrgId("test_org");
            rootReq.setName("测试公司");
            rootReq.setParentDeptId(null);
            rootReq.setSortOrder(0);
            rootReq.setLeaderUserId(testUserId);
            DepartmentDTO root = organizationService.createDepartment(rootReq);

            // 创建子部门1
            CreateDepartmentRequest dept1Req = new CreateDepartmentRequest();
            dept1Req.setOrgId("test_org");
            dept1Req.setName("技术部");
            dept1Req.setParentDeptId(root.getDeptId());
            dept1Req.setSortOrder(1);
            dept1Req.setLeaderUserId(testUserId);
            DepartmentDTO dept1 = organizationService.createDepartment(dept1Req);

            // 创建子部门2
            CreateDepartmentRequest dept2Req = new CreateDepartmentRequest();
            dept2Req.setOrgId("test_org");
            dept2Req.setName("产品部");
            dept2Req.setParentDeptId(root.getDeptId());
            dept2Req.setSortOrder(2);
            dept2Req.setLeaderUserId(testUserId);
            DepartmentDTO dept2 = organizationService.createDepartment(dept2Req);

            // 添加测试成员到技术部
            AddDepartmentMembersRequest addMembersReq = new AddDepartmentMembersRequest();
            addMembersReq.setUserIds(java.util.Collections.singletonList(testUserId));
            addMembersReq.setPrimaryDepartment(true);
            addMembersReq.setTitle("工程师");
            organizationService.addDepartmentMembers(dept1.getDeptId(), addMembersReq);

            Map<String, Object> data = new HashMap<>();
            data.put("rootDepartment", root);
            data.put("subDepartment1", dept1);
            data.put("subDepartment2", dept2);
            data.put("message", "测试数据初始化成功");

            return ResponseEntity.ok(Result.success("测试数据创建成功", data));
        } catch (Exception e) {
            log.error("初始化测试数据失败", e);
            return ResponseEntity.ok(Result.error(500, "初始化测试数据失败: " + e.getMessage()));
        }
    }

    /**
     * 验证部门树查询
     */
    @GetMapping("/verify-tree")
    public ResponseEntity<Result<Map<String, Object>>> verifyTree() {
        try {
            List<DepartmentTreeNodeDTO> tree = organizationService.getDepartmentTree();
            
            Map<String, Object> data = new HashMap<>();
            data.put("treeNodeCount", tree.size());
            data.put("tree", tree);
            data.put("status", tree.isEmpty() ? "EMPTY" : "OK");
            
            return ResponseEntity.ok(Result.success("部门树查询成功", data));
        } catch (Exception e) {
            log.error("验证部门树失败", e);
            return ResponseEntity.ok(Result.error(500, "验证部门树失败: " + e.getMessage()));
        }
    }

    /**
     * 验证组织通讯录
     */
    @GetMapping("/verify-contacts")
    public ResponseEntity<Result<Map<String, Object>>> verifyContacts() {
        try {
            List<DepartmentMemberDTO> contacts = organizationService.getContacts();
            
            Map<String, Object> data = new HashMap<>();
            data.put("contactCount", contacts.size());
            data.put("contacts", contacts);
            data.put("status", contacts.isEmpty() ? "EMPTY" : "OK");
            
            return ResponseEntity.ok(Result.success("通讯录查询成功", data));
        } catch (Exception e) {
            log.error("验证通讯录失败", e);
            return ResponseEntity.ok(Result.error(500, "验证通讯录失败: " + e.getMessage()));
        }
    }

    /**
     * 清理测试数据
     */
    @DeleteMapping("/cleanup-test-data")
    public ResponseEntity<Result<String>> cleanupTestData() {
        try {
            // 这里可以添加清理逻辑，删除 orgId="test_org" 的所有部门
            log.info("清理测试数据（当前为占位实现）");
            return ResponseEntity.ok(Result.success("测试数据清理完成"));
        } catch (Exception e) {
            log.error("清理测试数据失败", e);
            return ResponseEntity.ok(Result.error(500, "清理测试数据失败: " + e.getMessage()));
        }
    }
}
