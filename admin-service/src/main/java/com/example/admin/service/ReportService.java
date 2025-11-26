package com.example.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.entity.Report;
import com.example.admin.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 举报服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final ReportMapper reportMapper;
    
    /**
     * 创建举报
     */
    public Long createReport(Long reporterId, String targetType, String targetId, String reason) {
        Report report = new Report()
                .setReporterId(reporterId)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setReason(reason)
                .setStatus(Report.STATUS_PENDING)
                .setCreatedAt(LocalDateTime.now());
        reportMapper.insert(report);
        log.info("创建举报: type={}, targetId={}, reporter={}", targetType, targetId, reporterId);
        return report.getId();
    }
    
    /**
     * 处理举报
     */
    @Transactional
    public void handleReport(Long reportId, int status, String comment, Long adminId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        report.setStatus(status)
                .setAdminComment(comment)
                .setHandledBy(adminId);
        reportMapper.updateById(report);
        log.info("处理举报: id={}, status={}, admin={}", reportId, status, adminId);
    }
    
    /**
     * 分页查询举报
     */
    public Page<Report> listReports(int page, int size, Integer status) {
        LambdaQueryWrapper<Report> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(Report::getStatus, status);
        }
        query.orderByDesc(Report::getCreatedAt);
        return reportMapper.selectPage(new Page<>(page, size), query);
    }
    
    /**
     * 获取待处理举报数量
     */
    public int countPending() {
        return reportMapper.countPending();
    }
}
