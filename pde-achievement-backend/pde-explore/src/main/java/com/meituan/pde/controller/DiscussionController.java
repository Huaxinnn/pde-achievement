package com.meituan.pde.controller;

import com.meituan.pde.service.DiscussionService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/discussion")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    /**
     * 获取某关卡讨论列表
     * GET /ulivepde/api/discussion/posts?stageId=1
     */
    @GetMapping("/posts")
    public Map<String, Object> listPosts(@RequestParam Long stageId, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取讨论列表，stageId: {}, mis: {}", stageId, mis);
        List<DiscussionService.PostVO> posts = discussionService.listPosts(stageId, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("posts", posts);
        return result;
    }

    /**
     * 发布顶层帖子
     * POST /ulivepde/api/discussion/posts
     * body: { "stageId": 1, "content": "..." }
     */
    @PostMapping("/posts")
    public Map<String, Object> createPost(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        Long stageId = body.get("stageId") != null ? ((Number) body.get("stageId")).longValue() : null;
        String content = (String) body.get("content");

        if (stageId == null || content == null || content.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "stageId 和 content 不能为空");
            return err;
        }

        log.info("发布帖子，mis: {}, stageId: {}", mis, stageId);
        DiscussionService.PostVO post = discussionService.createPost(stageId, mis, content.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("post", post);
        return result;
    }

    /**
     * 回复帖子
     * POST /ulivepde/api/discussion/posts/{postId}/replies
     * body: { "content": "..." }
     */
    @PostMapping("/posts/{postId}/replies")
    public Map<String, Object> createReply(@PathVariable Long postId,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String content = (String) body.get("content");

        if (content == null || content.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "content 不能为空");
            return err;
        }

        log.info("回复帖子，mis: {}, postId: {}", mis, postId);
        DiscussionService.ReplyVO reply = discussionService.createReply(postId, mis, content.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("reply", reply);
        return result;
    }

    /**
     * 点赞 / 取消点赞（幂等）
     * POST /ulivepde/api/discussion/posts/{postId}/like
     */
    @PostMapping("/posts/{postId}/like")
    public Map<String, Object> toggleLike(@PathVariable Long postId, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("点赞/取消点赞，mis: {}, postId: {}", mis, postId);
        DiscussionService.LikeResult likeResult = discussionService.toggleLike(postId, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("liked", likeResult.isLiked());
        result.put("likeCount", likeResult.getLikeCount());
        return result;
    }
}
