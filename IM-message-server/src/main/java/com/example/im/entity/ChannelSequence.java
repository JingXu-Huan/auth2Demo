package com.example.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 频道序列号实体
 * 对应 im_db.channel_sequences 表
 */
@Data
@Accessors(chain = true)
@TableName("channel_sequences")
public class ChannelSequence {
    
    @TableId(type = IdType.INPUT)
    private Long channelId;
    
    private Long currentSeq;
    
    private LocalDateTime updatedAt;
}
