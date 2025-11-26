package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 消息搜索索引实体
 * 对应 im_db.message_search_index 表
 */
@Data
@Accessors(chain = true)
@TableName("message_search_index")
public class MessageSearchIndex {

    /** 消息ID */
    @TableId(type = IdType.INPUT)
    private Long messageId;

    /** 频道ID */
    private Long channelId;

    /** 发送者ID */
    private Long senderId;

    /** 全文搜索向量 (PostgreSQL tsvector类型) */
    private String contentTsv;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
