package com.example.relationship.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 好友申请实体类
 */
@Data
@Accessors(chain = true)
@TableName("friend_requests")
public class FriendRequest {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 发送者ID */
    private Long senderId;
    
    /** 接收者ID */
    private Long receiverId;
    
    /** 申请消息 */
    private String message;
    
    /** 备注名 */
    private String remark;
    
    /** 状态：0:待处理, 1:已同意, 2:已拒绝, 3:已忽略, 4:已过期 */
    private Integer status;
    
    /** 来源：search, qrcode, group, recommend, card */
    private String source;
    
    /** 来源ID（群ID、推荐人ID等）*/
    private String sourceId;
    
    /** 处理人ID */
    private Long handledBy;
    
    /** 处理时间 */
    private OffsetDateTime handledAt;
    
    /** 拒绝原因 */
    private String rejectReason;
    
    /** 过期时间 */
    private OffsetDateTime expiresAt;
    
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    // 状态常量
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_ACCEPTED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_IGNORED = 3;
    public static final int STATUS_EXPIRED = 4;
}
