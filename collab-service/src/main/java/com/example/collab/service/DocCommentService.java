package com.example.collab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.collab.entity.DocComment;
import com.example.collab.mapper.DocCommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档评论服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocCommentService {
    
    private final DocCommentMapper docCommentMapper;
    
    /**
     * 添加评论
     */
    @Transactional
    public DocComment addComment(Long docId, Long userId, String userName, String content, String anchor) {
        DocComment comment = new DocComment()
                .setDocId(docId)
                .setUserId(userId)
                .setUserName(userName)
                .setContent(content)
                .setAnchorText(anchor)
                .setLikeCount(0)
                .setReplyCount(0)
                .setIsResolved(false)
                .setIsPinned(false)
                .setCreatedAt(LocalDateTime.now());
        
        docCommentMapper.insert(comment);
        log.info("添加评论成功: docId={}, userId={}", docId, userId);
        return comment;
    }
    
    /**
     * 回复评论
     */
    @Transactional
    public DocComment replyComment(Long parentId, Long docId, Long userId, String userName, String content) {
        DocComment parent = docCommentMapper.selectById(parentId);
        if (parent == null) {
            throw new IllegalArgumentException("父评论不存在");
        }
        
        DocComment reply = new DocComment()
                .setDocId(docId)
                .setParentId(parentId)
                .setUserId(userId)
                .setUserName(userName)
                .setContent(content)
                .setLikeCount(0)
                .setReplyCount(0)
                .setIsResolved(false)
                .setIsPinned(false)
                .setCreatedAt(LocalDateTime.now());
        
        docCommentMapper.insert(reply);
        
        // 更新父评论的回复数
        parent.setReplyCount(parent.getReplyCount() + 1);
        docCommentMapper.updateById(parent);
        
        log.info("回复评论成功: parentId={}, userId={}", parentId, userId);
        return reply;
    }
    
    /**
     * 删除评论
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        DocComment comment = docCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalStateException("只能删除自己的评论");
        }
        
        comment.setDeletedAt(LocalDateTime.now());
        docCommentMapper.updateById(comment);
        log.info("删除评论成功: commentId={}", commentId);
    }
    
    /**
     * 获取文档的评论列表
     */
    public List<DocComment> getDocComments(Long docId) {
        return docCommentMapper.selectByDocId(docId);
    }
    
    /**
     * 获取评论的回复列表
     */
    public List<DocComment> getReplies(Long parentId) {
        return docCommentMapper.selectReplies(parentId);
    }
    
    /**
     * 解决评论
     */
    public void resolveComment(Long commentId) {
        DocComment comment = docCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        comment.setIsResolved(true);
        comment.setUpdatedAt(LocalDateTime.now());
        docCommentMapper.updateById(comment);
    }
    
    /**
     * 置顶评论
     */
    public void pinComment(Long commentId, boolean pin) {
        DocComment comment = docCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        comment.setIsPinned(pin);
        comment.setUpdatedAt(LocalDateTime.now());
        docCommentMapper.updateById(comment);
    }
}
