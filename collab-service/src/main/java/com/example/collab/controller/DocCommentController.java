package com.example.collab.controller;

import com.example.collab.entity.DocComment;
import com.example.collab.service.DocCommentService;
import com.example.domain.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档评论接口
 */
@RestController
@RequestMapping("/api/doc/comments")
@RequiredArgsConstructor
public class DocCommentController {
    
    private final DocCommentService docCommentService;
    
    /**
     * 添加评论
     */
    @PostMapping
    public Result<DocComment> addComment(
            @RequestParam Long docId,
            @RequestParam String content,
            @RequestParam(required = false) String anchor,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        DocComment comment = docCommentService.addComment(docId, userId, userName, content, anchor);
        return Result.success(comment);
    }
    
    /**
     * 回复评论
     */
    @PostMapping("/{parentId}/reply")
    public Result<DocComment> replyComment(
            @PathVariable Long parentId,
            @RequestParam Long docId,
            @RequestParam String content,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        DocComment reply = docCommentService.replyComment(parentId, docId, userId, userName, content);
        return Result.success(reply);
    }
    
    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("X-User-Id") Long userId) {
        docCommentService.deleteComment(commentId, userId);
        return Result.success(null);
    }
    
    /**
     * 获取文档评论
     */
    @GetMapping("/doc/{docId}")
    public Result<List<DocComment>> getDocComments(@PathVariable Long docId) {
        List<DocComment> comments = docCommentService.getDocComments(docId);
        return Result.success(comments);
    }
    
    /**
     * 获取评论回复
     */
    @GetMapping("/{commentId}/replies")
    public Result<List<DocComment>> getReplies(@PathVariable Long commentId) {
        List<DocComment> replies = docCommentService.getReplies(commentId);
        return Result.success(replies);
    }
    
    /**
     * 解决评论
     */
    @PostMapping("/{commentId}/resolve")
    public Result<Void> resolveComment(@PathVariable Long commentId) {
        docCommentService.resolveComment(commentId);
        return Result.success(null);
    }
    
    /**
     * 置顶评论
     */
    @PostMapping("/{commentId}/pin")
    public Result<Void> pinComment(
            @PathVariable Long commentId,
            @RequestParam boolean pin) {
        docCommentService.pinComment(commentId, pin);
        return Result.success(null);
    }
}
