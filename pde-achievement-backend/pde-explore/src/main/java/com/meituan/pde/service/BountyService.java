package com.meituan.pde.service;

import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.dao.BountyMemberDao;
import com.meituan.pde.dao.BountySubmissionDao;
import com.meituan.pde.dao.BountyTaskDao;
import com.meituan.pde.dao.BountyVoteDao;
import com.meituan.pde.dao.UserStageProgressDao;
import com.meituan.pde.entity.BountyMember;
import com.meituan.pde.entity.BountySubmission;
import com.meituan.pde.entity.BountyTask;
import com.meituan.pde.entity.BountyVote;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BountyService {

    @Autowired
    private BountyMemberDao bountyMemberDao;

    @Autowired
    private BountyTaskDao bountyTaskDao;

    @Autowired
    private BountySubmissionDao bountySubmissionDao;

    @Autowired
    private BountyVoteDao bountyVoteDao;

    @Autowired
    private UserStageProgressDao userStageProgressDao;

    @Autowired
    private SsoService ssoService;

    // ---- 任务查询 ----

    /**
     * 懒惰状态流转：读取时检查截止时间，自动推进过期阶段。
     * design_open  → design_voting (designDeadline 过了)
     * design_voting → design_closed (votingEnd 过了)
     * dev_open     → closed         (devDeadline 过了)
     * design_closed → dev_open 需要官方选 winner 后手动推进，不自动。
     */
    private void autoAdvance(BountyTask task) {
        if (task == null) return;
        String status = task.getStatus();
        Date now = new Date();
        String next = null;

        if ("design_open".equals(status) && task.getDesignDeadline() != null && now.after(task.getDesignDeadline())) {
            next = "design_voting";
        } else if ("design_voting".equals(status) && task.getVotingEnd() != null && now.after(task.getVotingEnd())) {
            next = "design_closed";
        } else if ("dev_open".equals(status) && task.getDevDeadline() != null && now.after(task.getDevDeadline())) {
            next = "closed";
        }

        if (next != null) {
            log.info("任务自动流转 taskId={} {} -> {}", task.getId(), status, next);
            bountyTaskDao.updateStatus(task.getId(), next);
            task.setStatus(next);
        }
    }

    public List<TaskVO> listTasks(String type, String status, String currentMis) {
        List<BountyTask> tasks = bountyTaskDao.findAll(type, status);
        tasks.forEach(this::autoAdvance);

        // 批量查出当前用户已点赞的 taskId 集合
        Set<Long> likedIds = (currentMis != null && !currentMis.isEmpty())
                ? new java.util.HashSet<>(bountyVoteDao.findLikedTaskIdsByUser(currentMis))
                : java.util.Collections.emptySet();

        // 批量查出当前用户的参与状态（提交 + 投票）
        List<Long> taskIds = tasks.stream().map(BountyTask::getId).collect(Collectors.toList());
        Map<Long, String> participationMap = new HashMap<>();
        if (currentMis != null && !currentMis.isEmpty() && !taskIds.isEmpty()) {
            // 查提交记录：dev > design 优先级
            List<BountySubmission> mySubmissions = bountySubmissionDao.findByUserMisAndTaskIds(currentMis, taskIds);
            for (BountySubmission s : mySubmissions) {
                String existing = participationMap.get(s.getTaskId());
                if ("dev".equals(s.getPhase())) {
                    participationMap.put(s.getTaskId(), "dev_submitted");
                } else if ("design".equals(s.getPhase()) && !"dev_submitted".equals(existing)) {
                    participationMap.put(s.getTaskId(), "design_submitted");
                }
            }
            // 查投票记录：如果没有更高优先级的提交状态，才标记为 voted
            List<Long> votedTaskIds = bountyVoteDao.findVotedTaskIdsByUser(currentMis, taskIds);
            for (Long tid : votedTaskIds) {
                if (!participationMap.containsKey(tid)) {
                    participationMap.put(tid, "voted");
                }
            }
        }

        return tasks.stream().map(t -> {
            TaskVO vo = toTaskVO(t, currentMis);
            vo.setParticipantCount(bountySubmissionDao.countByTaskId(t.getId()));
            vo.setLiked(likedIds.contains(t.getId()));
            vo.setMyParticipationStatus(participationMap.getOrDefault(t.getId(), null));
            return vo;
        }).collect(Collectors.toList());
    }

    public TaskDetailVO getTaskDetail(Long taskId, String currentMis) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        autoAdvance(task);

        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(toTaskVO(task, currentMis));

        // 用户已投票的 submissionId 集合（多票）
        List<BountyVote> myVotes = bountyVoteDao.findByTaskIdAndUser(taskId, currentMis);
        Set<Long> myVotedIds = myVotes.stream().map(BountyVote::getSubmissionId).collect(Collectors.toSet());
        int myVoteCount = myVotes.size();

        // 设计阶段提交列表
        List<BountySubmission> designSubmissions = bountySubmissionDao.findByTaskIdAndPhase(taskId, "design");

        // 构建每个 submission 的投票人列表（谁投了我）
        // key: submissionId, value: 投票人 mis 列表
        List<BountyVote> allVotes = bountyVoteDao.findByTaskId(taskId);
        Map<Long, List<String>> votersMap = new HashMap<>();
        for (BountyVote v : allVotes) {
            votersMap.computeIfAbsent(v.getSubmissionId(), k -> new ArrayList<>()).add(v.getUserMis());
        }

        // 批量获取所有提交者 + 投票者的用户名，避免 N+1
        Set<String> allMisSet = new java.util.HashSet<>();
        designSubmissions.forEach(s -> allMisSet.add(s.getUserMis()));
        allVotes.forEach(v -> allMisSet.add(v.getUserMis()));
        List<String> allMisList = new ArrayList<>(allMisSet);
        Map<String, SsoUserInfo> userInfoMap = allMisList.isEmpty()
                ? new HashMap<>() : ssoService.batchGetUserInfo(allMisList);

        vo.setDesignSubmissions(designSubmissions.stream()
                .map(s -> toSubmissionVO(s, currentMis, myVotedIds, userInfoMap, votersMap))
                .collect(Collectors.toList()));

        // 开发阶段提交列表（不需要 voters）
        List<BountySubmission> devSubmissions = bountySubmissionDao.findByTaskIdAndPhase(taskId, "dev");
        List<String> devMisList = devSubmissions.stream()
                .map(BountySubmission::getUserMis).distinct().collect(Collectors.toList());
        Map<String, SsoUserInfo> devUserInfoMap = devMisList.isEmpty()
                ? new HashMap<>() : ssoService.batchGetUserInfo(devMisList);
        vo.setDevSubmissions(devSubmissions.stream()
                .map(s -> toSubmissionVO(s, currentMis, null, devUserInfoMap, null))
                .collect(Collectors.toList()));

        // 当前用户提交状态
        BountySubmission myDesign = bountySubmissionDao.findByTaskIdAndPhaseAndUser(taskId, "design", currentMis);
        BountySubmission myDev = bountySubmissionDao.findByTaskIdAndPhaseAndUser(taskId, "dev", currentMis);
        vo.setMyDesignSubmission(myDesign != null ? toSubmissionVO(myDesign, currentMis, myVotedIds, userInfoMap, votersMap) : null);
        vo.setMyDevSubmission(myDev != null ? toSubmissionVO(myDev, currentMis, null, devUserInfoMap, null) : null);
        vo.setMyVotedSubmissionIds(new ArrayList<>(myVotedIds));
        vo.setMyVoteCount(myVoteCount);

        // 开发阶段门槛检查
        Long maxPassedStage = userStageProgressDao.findMaxPassedStageIdByUserMis(currentMis);
        int passedCount = maxPassedStage != null ? maxPassedStage.intValue() : 0;
        vo.setCanJoinDev(passedCount >= (task.getDevMinStage() != null ? task.getDevMinStage() : 5L));
        vo.setDevMinStage(task.getDevMinStage());

        return vo;
    }

    // ---- 任务管理（管理员） ----

    public BountyTask createTask(String title, String description, String type, String status,
                                  String createdBy, Date designDeadline, Date votingEnd,
                                  Date devDeadline, Long devMinStage, String rewardDesc) {
        BountyTask task = new BountyTask();
        task.setTitle(title);
        task.setDescription(description);
        task.setType(type != null ? type : "official");
        task.setStatus(status != null ? status : "draft");
        task.setCreatedBy(createdBy);
        task.setDesignDeadline(designDeadline);
        task.setVotingEnd(votingEnd);
        task.setDevDeadline(devDeadline);
        task.setDevMinStage(devMinStage != null ? devMinStage : 5L);
        task.setRewardDesc(rewardDesc != null ? rewardDesc : "");
        task.setCoverUrl("");
        task.setRefLink("");
        bountyTaskDao.insert(task);
        return task;
    }

    public void updateTaskStatus(Long taskId, String status) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        bountyTaskDao.updateStatus(taskId, status);
    }

    public void updateTask(Long taskId, String title, String description,
                            Date designDeadline, Date votingEnd, Date devDeadline,
                            Long devMinStage, String rewardDesc) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        task.setTitle(title);
        task.setDescription(description);
        task.setDesignDeadline(designDeadline);
        task.setVotingEnd(votingEnd);
        task.setDevDeadline(devDeadline);
        task.setDevMinStage(devMinStage != null ? devMinStage : 5L);
        task.setRewardDesc(rewardDesc != null ? rewardDesc : "");
        bountyTaskDao.updateTask(task);
    }

    public void deleteTask(Long taskId) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        bountyTaskDao.softDelete(taskId);
    }

    public void featureIdea(Long taskId, String adminMis, String reason) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null || !"idea".equals(task.getType())) {
            throw new IllegalArgumentException("Idea不存在");
        }
        bountyTaskDao.updateFeatured(taskId, adminMis, reason);
    }

    // ---- 相似标题检测 ----

    public List<BountyTask> findSimilarIdeas(String title, Long excludeId) {
        if (title == null || title.trim().length() < 2) {
            return java.util.Collections.emptyList();
        }
        // 滑动窗口拆词：把标题拆成2字片段，用 OR 多条件匹配
        // 例如 "排行榜" → ["排行", "行榜"]，"个人闯关排行版" → ["个人","闯关","排行","行版"]
        // 这样 "排行榜" 和 "个人闯关排行版" 能通过 "排行" 匹配上
        String cleaned = title.trim().replaceAll("[\\s\\p{Punct}]+", "");
        if (cleaned.length() < 2) {
            return java.util.Collections.emptyList();
        }
        List<String> keywords = new java.util.ArrayList<>();
        int step = 2;
        for (int i = 0; i <= cleaned.length() - step; i += step) {
            String seg = cleaned.substring(i, Math.min(i + step, cleaned.length()));
            if (seg.length() >= 2 && !keywords.contains(seg)) {
                keywords.add(seg);
            }
            if (keywords.size() >= 5) break; // 最多5个片段，避免SQL太长
        }
        if (keywords.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return bountyTaskDao.findSimilarIdeasByKeywords(keywords, excludeId);
    }

    // ---- 用户发布 Idea ----

    public BountyTask createIdea(String title, String description, String coverUrl, String refLink, String createdBy) {
        BountyTask task = new BountyTask();
        task.setTitle(title);
        task.setDescription(description);
        task.setCoverUrl(coverUrl != null ? coverUrl : "");
        task.setRefLink(refLink != null ? refLink : "");
        task.setType("idea");
        task.setStatus("design_open");
        task.setCreatedBy(createdBy);
        task.setDevMinStage(5L);
        task.setRewardDesc("");
        task.setDesignDeadline(new java.util.Date(0));
        task.setVotingEnd(new java.util.Date(0));
        task.setDevDeadline(new java.util.Date(0));
        bountyTaskDao.insert(task);
        return task;
    }

    public void updateIdea(Long id, String title, String description, String coverUrl, String refLink, String userMis) {
        BountyTask task = bountyTaskDao.findById(id);
        if (task == null || !"idea".equals(task.getType())) {
            throw new IllegalArgumentException("创意不存在");
        }
        if (!task.getCreatedBy().equals(userMis)) {
            throw new IllegalStateException("只能编辑自己发布的创意");
        }
        task.setTitle(title);
        task.setDescription(description);
        task.setCoverUrl(coverUrl != null ? coverUrl : "");
        task.setRefLink(refLink != null ? refLink : "");
        bountyTaskDao.updateIdea(task);
    }

    public void deleteIdea(Long id, String userMis) {
        BountyTask task = bountyTaskDao.findById(id);
        if (task == null || !"idea".equals(task.getType())) {
            throw new IllegalArgumentException("创意不存在");
        }
        if (!task.getCreatedBy().equals(userMis)) {
            throw new IllegalStateException("只能删除自己发布的创意");
        }
        bountyTaskDao.softDelete(id);
    }

    // ---- 点赞（复用 bounty_vote 表）----

    @Transactional
    public boolean toggleLike(Long taskId, String userMis) {
        // 用 bounty_vote 表，submission_id=0 表示赞的是任务/创意本身
        BountyVote existing = bountyVoteDao.findByTaskIdAndSubmissionAndUser(taskId, 0L, userMis);
        if (existing != null) {
            // 取消点赞
            bountyVoteDao.deleteByTaskIdAndSubmissionAndUser(taskId, 0L, userMis);
            bountyTaskDao.decrementLikeCount(taskId);
            return false;
        } else {
            // 点赞
            BountyVote vote = new BountyVote();
            vote.setTaskId(taskId);
            vote.setSubmissionId(0L);
            vote.setUserMis(userMis);
            bountyVoteDao.insert(vote);
            bountyTaskDao.incrementLikeCount(taskId);
            return true;
        }
    }

    public boolean hasLiked(Long taskId, String userMis) {
        return bountyVoteDao.findByTaskIdAndSubmissionAndUser(taskId, 0L, userMis) != null;
    }

    // ---- 提交 ----

    @Transactional
    public BountySubmission submitDesign(Long taskId, String userMis, String title, String url, String description) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!"design_open".equals(task.getStatus())) {
            throw new IllegalStateException("当前阶段不接受设计方案提交");
        }

        BountySubmission existing = bountySubmissionDao.findByTaskIdAndPhaseAndUser(taskId, "design", userMis);
        if (existing != null) {
            // 已提交过，更新
            existing.setTitle(title);
            existing.setUrl(url);
            existing.setDescription(description);
            bountySubmissionDao.update(existing);
            return existing;
        }

        BountySubmission submission = new BountySubmission();
        submission.setTaskId(taskId);
        submission.setPhase("design");
        submission.setUserMis(userMis);
        submission.setTitle(title);
        submission.setUrl(url);
        submission.setRepoUrl("");
        submission.setDescription(description);
        bountySubmissionDao.insert(submission);
        return submission;
    }

    @Transactional
    public BountySubmission submitDev(Long taskId, String userMis, String title,
                                       String url, String repoUrl, String description) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (!"dev_open".equals(task.getStatus())) {
            throw new IllegalStateException("当前阶段不接受开发作品提交");
        }

        // 检查开发门槛
        Long maxStage = userStageProgressDao.findMaxPassedStageIdByUserMis(userMis);
        int passedCount = maxStage != null ? maxStage.intValue() : 0;
        long minStage = task.getDevMinStage() != null ? task.getDevMinStage() : 5L;
        if (passedCount < minStage) {
            throw new IllegalStateException("需要通过第" + minStage + "关以上才能参与开发");
        }

        BountySubmission existing = bountySubmissionDao.findByTaskIdAndPhaseAndUser(taskId, "dev", userMis);
        if (existing != null) {
            existing.setTitle(title);
            existing.setUrl(url);
            existing.setRepoUrl(repoUrl != null ? repoUrl : "");
            existing.setDescription(description);
            bountySubmissionDao.update(existing);
            return existing;
        }

        BountySubmission submission = new BountySubmission();
        submission.setTaskId(taskId);
        submission.setPhase("dev");
        submission.setUserMis(userMis);
        submission.setTitle(title);
        submission.setUrl(url);
        submission.setRepoUrl(repoUrl != null ? repoUrl : "");
        submission.setDescription(description);
        bountySubmissionDao.insert(submission);
        return submission;
    }

    // ---- 投票（设计阶段，每人1票，可取消后改投） ----

    @Transactional
    public VoteResult vote(Long taskId, Long submissionId, String userMis) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        if (!"design_voting".equals(task.getStatus())) throw new IllegalStateException("当前不在投票阶段");

        BountySubmission submission = bountySubmissionDao.findById(submissionId);
        if (submission == null || !submission.getTaskId().equals(taskId)) throw new IllegalArgumentException("提交不存在");
        if (userMis.equals(submission.getUserMis())) throw new IllegalStateException("不能给自己的方案投票");

        // 已经投过这个方案了
        BountyVote existing = bountyVoteDao.findByTaskIdAndSubmissionAndUser(taskId, submissionId, userMis);
        if (existing != null) throw new IllegalStateException("已投过该方案，如需取消请撤票");

        // 已投过其他方案则先撤票（1票制，唯一约束保证每任务每人只有一条记录）
        List<BountyVote> currentVotes = bountyVoteDao.findByTaskIdAndUser(taskId, userMis);
        for (BountyVote old : currentVotes) {
            bountyVoteDao.deleteByTaskIdAndSubmissionAndUser(taskId, old.getSubmissionId(), userMis);
            bountySubmissionDao.decrementVoteCount(old.getSubmissionId());
        }

        BountyVote vote = new BountyVote();
        vote.setTaskId(taskId);
        vote.setSubmissionId(submissionId);
        vote.setUserMis(userMis);
        bountyVoteDao.insert(vote);
        bountySubmissionDao.incrementVoteCount(submissionId);

        VoteResult result = new VoteResult();
        result.setSubmissionId(submissionId);
        result.setVoteCount(bountySubmissionDao.findById(submissionId).getVoteCount());
        result.setMyVoteCount(1);
        return result;
    }

    @Transactional
    public VoteResult unvote(Long taskId, Long submissionId, String userMis) {
        BountyTask task = bountyTaskDao.findById(taskId);
        if (task == null) throw new IllegalArgumentException("任务不存在");
        if (!"design_voting".equals(task.getStatus())) throw new IllegalStateException("当前不在投票阶段");

        BountyVote existing = bountyVoteDao.findByTaskIdAndSubmissionAndUser(taskId, submissionId, userMis);
        if (existing == null) throw new IllegalStateException("未投过该方案");

        bountyVoteDao.deleteByTaskIdAndSubmissionAndUser(taskId, submissionId, userMis);
        bountySubmissionDao.decrementVoteCount(submissionId);

        VoteResult result = new VoteResult();
        result.setSubmissionId(submissionId);
        result.setVoteCount(bountySubmissionDao.findById(submissionId).getVoteCount());
        result.setMyVoteCount(bountyVoteDao.countByTaskIdAndUser(taskId, userMis));
        return result;
    }

    // ---- 管理员审核 ----

    public void reviewSubmission(Long submissionId, String status, String rejectReason) {
        BountySubmission submission = bountySubmissionDao.findById(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("提交不存在");
        }
        bountySubmissionDao.updateReview(submissionId, status, rejectReason != null ? rejectReason : "");
    }

    @Transactional
    public void setWinner(Long submissionId) {
        BountySubmission submission = bountySubmissionDao.findById(submissionId);
        if (submission == null) {
            throw new IllegalArgumentException("提交不存在");
        }
        bountySubmissionDao.setWinner(submissionId, submission.getTaskId(), submission.getPhase());
    }

    // ---- VO 转换 ----

    private TaskVO toTaskVO(BountyTask task, String currentMis) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setType(task.getType());
        vo.setStatus(task.getStatus());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setDesignDeadline(formatDate(task.getDesignDeadline()));
        vo.setVotingEnd(formatDate(task.getVotingEnd()));
        vo.setDevDeadline(formatDate(task.getDevDeadline()));
        vo.setDevMinStage(task.getDevMinStage());
        vo.setRewardDesc(task.getRewardDesc());
        vo.setCoverUrl(task.getCoverUrl());
        vo.setRefLink(task.getRefLink());
        vo.setLikeCount(task.getLikeCount() != null ? task.getLikeCount() : 0);
        vo.setFeatured(Integer.valueOf(1).equals(task.getIsFeatured()));
        vo.setFeaturedBy(task.getFeaturedBy());
        vo.setFeaturedReason(task.getFeaturedReason());
        vo.setCreateTime(formatDate(task.getCreateTime() != null ? task.getCreateTime() : task.getAddTime()));
        return vo;
    }

    private SubmissionVO toSubmissionVO(BountySubmission s, String currentMis,
                                         Set<Long> myVotedIds,
                                         Map<String, SsoUserInfo> userInfoMap,
                                         Map<Long, List<String>> votersMap) {
        SubmissionVO vo = new SubmissionVO();
        vo.setId(s.getId());
        vo.setTaskId(s.getTaskId());
        vo.setPhase(s.getPhase());
        vo.setUserMis(s.getUserMis());
        // 用户显示名（用于前端搜索）
        SsoUserInfo info = userInfoMap != null ? userInfoMap.get(s.getUserMis()) : null;
        vo.setUserName(info != null ? info.getName() : s.getUserMis());
        vo.setTitle(s.getTitle());
        vo.setUrl(s.getUrl());
        vo.setRepoUrl(s.getRepoUrl());
        vo.setDescription(s.getDescription());
        vo.setStatus(s.getStatus());
        vo.setRejectReason(s.getRejectReason());
        vo.setWinner(Integer.valueOf(1).equals(s.getIsWinner()));
        vo.setVoteCount(s.getVoteCount() != null ? s.getVoteCount() : 0);
        vo.setMySubmission(s.getUserMis().equals(currentMis));
        vo.setMyVote(myVotedIds != null && myVotedIds.contains(s.getId()));
        vo.setCreateTime(formatDate(s.getCreateTime() != null ? s.getCreateTime() : s.getAddTime()));
        // 投票人列表（仅当前用户自己的方案才填充，其他人方案不暴露）
        if (s.getUserMis().equals(currentMis) && votersMap != null) {
            List<String> voterMisList = votersMap.getOrDefault(s.getId(), new ArrayList<>());
            if (!voterMisList.isEmpty() && userInfoMap != null) {
                List<Map<String, String>> voters = voterMisList.stream().map(mis -> {
                    Map<String, String> v = new HashMap<>();
                    v.put("mis", mis);
                    SsoUserInfo vi = userInfoMap.get(mis);
                    v.put("name", vi != null ? vi.getName() : mis);
                    return v;
                }).collect(Collectors.toList());
                vo.setVoters(voters);
            } else {
                vo.setVoters(new ArrayList<>());
            }
        }
        return vo;
    }

    private String formatDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    // ---- VO / Result 内部类 ----

    @Data
    public static class TaskVO {
        private Long id;
        private String title;
        private String description;
        private String type;
        private String status;
        private String createdBy;
        private String designDeadline;
        private String votingEnd;
        private String devDeadline;
        private Long devMinStage;
        private String rewardDesc;
        private String coverUrl;
        private String refLink;
        private long likeCount;
        private boolean liked;  // 当前用户是否已点赞
        private boolean featured;
        private String featuredBy;
        private String featuredReason;
        private String createTime;
        private int participantCount;
        private String myParticipationStatus;  // 当前用户参与状态：voted / design_submitted / dev_submitted / null
    }

    @Data
    public static class TaskDetailVO {
        private TaskVO task;
        private List<SubmissionVO> designSubmissions;
        private List<SubmissionVO> devSubmissions;
        private SubmissionVO myDesignSubmission;
        private SubmissionVO myDevSubmission;
        private List<Long> myVotedSubmissionIds;  // 已投票的方案 id 列表（多票）
        private int myVoteCount;                  // 已用票数
        private boolean canJoinDev;
        private Long devMinStage;
    }

    @Data
    public static class SubmissionVO {
        private Long id;
        private Long taskId;
        private String phase;
        private String userMis;
        private String userName;           // 用户显示名，供前端搜索
        private String title;
        private String url;
        private String repoUrl;
        private String description;
        private String status;
        private String rejectReason;
        private boolean winner;
        private long voteCount;
        private boolean mySubmission;
        private boolean myVote;
        private String createTime;
        private List<Map<String, String>> voters;  // 投了我的人（仅自己的方案返回）
    }

    @Data
    public static class VoteResult {
        private Long submissionId;
        private long voteCount;
        private int myVoteCount;  // 操作后用户剩余已用票数
    }

    // ---- 加入共建 ----

    /**
     * 用户加入悬赏共建。
     * 如果已加入过，更新信息；否则新增。
     */
    @Transactional
    public void joinBounty(String mis, String userName, String dept, String role, String customRole, String reason) {
        BountyMember existing = bountyMemberDao.findByUserMis(mis);
        if (existing != null) {
            // 已存在，更新信息
            existing.setUserName(userName);
            existing.setDept(dept);
            existing.setRole(role);
            existing.setCustomRole(customRole != null ? customRole : "");
            existing.setReason(reason != null ? reason : "");
            bountyMemberDao.updateByUserMis(existing);
            log.info("更新共建成员信息，mis: {}", mis);
        } else {
            // 新成员
            BountyMember member = new BountyMember();
            member.setUserMis(mis);
            member.setUserName(userName);
            member.setDept(dept);
            member.setRole(role);
            member.setCustomRole(customRole != null ? customRole : "");
            member.setReason(reason != null ? reason : "");
            bountyMemberDao.insert(member);
            log.info("新增共建成员，mis: {}", mis);
        }
    }

    /**
     * 查询用户是否已加入共建
     */
    public boolean isMember(String mis) {
        return bountyMemberDao.findByUserMis(mis) != null;
    }

    /**
     * 获取共建成员总数
     */
    public int getMemberCount() {
        return bountyMemberDao.countAll();
    }

    /**
     * 获取用户的加入排名（第几个加入的）
     */
    public int getMemberRank(String mis) {
        return bountyMemberDao.countBefore(mis) + 1;
    }

    /**
     * 获取所有共建成员列表
     */
    public List<BountyMember> getAllMembers() {
        return bountyMemberDao.findAll();
    }
}
