package org.example.imgroupserver.controller;

import com.example.domain.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class HealthController {

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Result<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "IM-group-server");
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now());
        data.put("version", "1.0.0");
        
        return ResponseEntity.ok(Result.success("Service is healthy", data));
    }

    /**
     * 服务信息端点
     */
    @GetMapping("/info")
    public ResponseEntity<Result<Map<String, Object>>> info() {
        Map<String, Object> data = new HashMap<>();
        data.put("service", "IM-group-server");
        data.put("description", "IM Group Management Service");
        data.put("version", "1.0.0");
        data.put("features", new String[]{
            "Group Creation",
            "Member Management", 
            "Admin Management",
            "Group Information"
        });
        
        return ResponseEntity.ok(Result.success("Service information", data));
    }
}
