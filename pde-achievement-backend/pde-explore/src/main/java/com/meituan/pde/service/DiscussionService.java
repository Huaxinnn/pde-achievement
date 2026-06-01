package com.meituan.pde.service;

import com.meituan.pde.dao.DiscussionLikeDao;
import com.meituan.pde.dao.DiscussionPostDao;
import com.meituan.pde.entity.DiscussionLike;
import com.meituan.pde.entity.DiscussionPost;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscussionService {

    @Autowired
    private DiscussionPostDao discussionPostDao;

    @Autowired
    private DiscussionLikeDao discussionLikeDao;

    /**
     * 获取某关卡的讨论列表（帖子 + 回复 + 当前用户点赞状态）
     */
    public List<PostVO> listPosts(Long stageId, String currentMis) {
        List<DiscussionPost> tops = discussionPostDao.findTopByStageId(stageId);
        if (tops.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> topIds = tops.stream().map(DiscussionPost::getId).collect(Collectors.toList());

        // 批量拉回复
        List<DiscussionPost> replies = topIds.isEmpty()
                ? Collections.emptyList()
                : discussionPostDao.findRepliesByParentIds(topIds);

        // 收集所有 postId，批量查点赞
        List<Long> allIds = new ArrayList<>(topIds);
        replies.forEach(r -> allIds.add(r.getId()));
        Set<Long> likedIds = new HashSet<>();
        if (!allIds.isEmpty()) {
            List<DiscussionLike> likes = discussionLikeDao.findByPostIdsAndUserMis(allIds, currentMis);
            likes.forEach(l -> likedIds.add(l.getPostId()));
        }

        // 按 parentId 分组回复
        Map<Long, List<DiscussionPost>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(DiscussionPost::getParentId));

        return tops.stream().map(top -> {
            PostVO vo = toPostVO(top, likedIds);
            List<DiscussionPost> childPosts = replyMap.getOrDefault(top.getId(), Collections.emptyList());
            vo.setReplies(childPosts.stream().map(r -> toReplyVO(r, likedIds)).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 发帖（顶层）
     */
    public PostVO createPost(Long stageId, String userMis, String content) {
        DiscussionPost post = new DiscussionPost();
        post.setStageId(stageId);
        post.setParentId(0L);
        post.setUserMis(userMis);
        post.setContent(content);
        discussionPostDao.insert(post);

        PostVO vo = toPostVO(post, Collections.emptySet());
        vo.setReplies(Collections.emptyList());
        return vo;
    }

    /**
     * 回复帖子
     */
    public ReplyVO createReply(Long postId, String userMis, String content) {
        DiscussionPost parent = discussionPostDao.findById(postId);
        if (parent == null) {
            throw new IllegalArgumentException("帖子不存在");
        }

        DiscussionPost reply = new DiscussionPost();
        reply.setStageId(parent.getStageId());
        reply.setParentId(postId);
        reply.setUserMis(userMis);
        reply.setContent(content);
        discussionPostDao.insert(reply);

        return toReplyVO(reply, Collections.emptySet());
    }

    /**
     * 点赞 / 取消点赞（幂等）
     */
    @Transactional
    public LikeResult toggleLike(Long postId, String userMis) {
        DiscussionPost post = discussionPostDao.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }

        DiscussionLike existing = discussionLikeDao.findByPostIdAndUserMis(postId, userMis);
        boolean liked;
        if (existing == null) {
            DiscussionLike like = new DiscussionLike();
            like.setPostId(postId);
            like.setUserMis(userMis);
            discussionLikeDao.insert(like);
            discussionPostDao.incrementLikeCount(postId);
            liked = true;
        } else {
            discussionLikeDao.deleteByPostIdAndUserMis(postId, userMis);
            discussionPostDao.decrementLikeCount(postId);
            liked = false;
        }

        DiscussionPost updated = discussionPostDao.findById(postId);
        LikeResult result = new LikeResult();
        result.setLiked(liked);
        result.setLikeCount(updated != null && updated.getLikeCount() != null ? updated.getLikeCount() : 0L);
        return result;
    }

    // ---- VO 转换 ----

    private PostVO toPostVO(DiscussionPost post, Set<Long> likedIds) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setAuthor(post.getUserMis());
        vo.setContent(post.getContent());
        vo.setTime(formatTime(post.getCreateTime() != null ? post.getCreateTime() : post.getAddTime()));
        vo.setLikes(post.getLikeCount() != null ? post.getLikeCount() : 0L);
        vo.setLiked(likedIds.contains(post.getId()));
        return vo;
    }

    private ReplyVO toReplyVO(DiscussionPost reply, Set<Long> likedIds) {
        ReplyVO vo = new ReplyVO();
        vo.setId(reply.getId());
        vo.setAuthor(reply.getUserMis());
        vo.setContent(reply.getContent());
        vo.setTime(formatTime(reply.getCreateTime() != null ? reply.getCreateTime() : reply.getAddTime()));
        vo.setLikes(reply.getLikeCount() != null ? reply.getLikeCount() : 0L);
        vo.setLiked(likedIds.contains(reply.getId()));
        return vo;
    }

    private String formatTime(Date date) {
        if (date == null) return "";
        long diff = System.currentTimeMillis() - date.getTime();
        long minutes = diff / 60000;
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        long hours = minutes / 60;
        if (hours < 24) return hours + "小时前";
        long days = hours / 24;
        if (days < 30) return days + "天前";
        return new SimpleDateFormat("MM-dd").format(date);
    }

    // ---- VO / Result 内部类 ----

    @Data
    public static class PostVO {
        private Long id;
        private String author;
        private String content;
        private String time;
        private long likes;
        private boolean liked;
        private List<ReplyVO> replies;
    }

    @Data
    public static class ReplyVO {
        private Long id;
        private String author;
        private String content;
        private String time;
        private long likes;
        private boolean liked;
    }

    @Data
    public static class LikeResult {
        private boolean liked;
        private long likeCount;
    }
}
