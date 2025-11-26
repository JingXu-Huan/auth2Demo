package com.example.job.task;

import com.xxl.job.core.handler.annotation.XxlJob;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 文件清理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {
    
    private final JdbcTemplate jdbcTemplate;
    private final MinioClient minioClient;
    
    @Value("${minio.bucket:files}")
    private String bucket;
    
    /**
     * 清理孤儿文件（ref_count=0的文件）
     * 每日凌晨3点执行
     */
    @XxlJob("cleanOrphanFiles")
    public void cleanOrphanFiles() {
        log.info("开始清理孤儿文件...");
        
        // 查询ref_count=0且超过7天的文件
        String sql = """
            SELECT id, storage_path FROM file_metadata 
            WHERE ref_count = 0 AND updated_at < NOW() - INTERVAL '7 days'
            LIMIT 100
            """;
        
        List<Map<String, Object>> files = jdbcTemplate.queryForList(sql);
        int successCount = 0;
        
        for (Map<String, Object> file : files) {
            Long id = (Long) file.get("id");
            String storagePath = (String) file.get("storage_path");
            
            try {
                // 从MinIO删除
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(storagePath)
                        .build());
                
                // 从数据库删除记录
                jdbcTemplate.update("DELETE FROM file_metadata WHERE id = ?", id);
                successCount++;
            } catch (Exception e) {
                log.error("删除文件失败: {}", storagePath, e);
            }
        }
        
        log.info("孤儿文件清理完成，成功删除: {} 个", successCount);
    }
    
    /**
     * 清理过期的上传会话
     */
    @XxlJob("cleanExpiredUploadSessions")
    public void cleanExpiredUploadSessions() {
        log.info("开始清理过期上传会话...");
        
        String sql = """
            DELETE FROM file_upload_sessions 
            WHERE status != 'COMPLETED' AND created_at < NOW() - INTERVAL '24 hours'
            """;
        
        int count = jdbcTemplate.update(sql);
        log.info("过期上传会话清理完成，删除: {} 条", count);
    }
}
