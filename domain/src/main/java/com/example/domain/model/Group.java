package com.example.domain.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "im_group")
@TableName("im_group")
public class Group {

    @Id
    @TableId(value = "group_id", type = IdType.INPUT)
    @Column(name = "group_id", nullable = false, length = 64)
    private String groupId;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(length = 256)
    private String avatar;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_type", nullable = false, length = 32)
    private JoinType joinType;

    @Column(length = 1024)
    private String announcement;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum JoinType {
        FREE,
        APPROVAL,
        INVITE_ONLY
    }
}
