package com.meituan.pde.controller;

import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.service.AdminService;
import com.meituan.pde.entity.BountyMember;
import com.meituan.pde.entity.BountyTask;
import com.meituan.pde.service.BountyService;
import com.meituan.pde.service.SsoService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/bounty")
public class BountyController {

    @Autowired
    private BountyService bountyService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private SsoService ssoService;

    /**
     * 任务列表
     * GET /ulivepde/api/bounty/tasks?type=official&status=design_open
     */
    @GetMapping("/tasks")
    public Map<String, Object> listTasks(@RequestParam(required = false) String type,
                                          @RequestParam(required = false) String status,
                                          HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        List<BountyService.TaskVO> tasks = bountyService.listTasks(type, status, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("tasks", tasks);
        return result;
    }

    /**
     * 任务详情
     * GET /ulivepde/api/bounty/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    public Map<String, Object> getTaskDetail(@PathVariable Long taskId, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        BountyService.TaskDetailVO detail = bountyService.getTaskDetail(taskId, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("detail", detail);
        return result;
    }

    /**
     * 提交设计方案
     * POST /ulivepde/api/bounty/tasks/{taskId}/design-submit
     * body: { "title": "...", "url": "...", "description": "..." }
     */
    @PostMapping("/tasks/{taskId}/design-submit")
    public Map<String, Object> submitDesign(@PathVariable Long taskId,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String title = (String) body.get("title");
        String url = (String) body.get("url");
        String description = (String) body.get("description");

        if (title == null || title.trim().isEmpty() || url == null || url.trim().isEmpty()) {
            return error("title 和 url 不能为空");
        }

        log.info("提交设计方案，mis: {}, taskId: {}", mis, taskId);
        bountyService.submitDesign(taskId, mis, title.trim(), url.trim(),
                description != null ? description.trim() : "");
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 提交开发作品
     * POST /ulivepde/api/bounty/tasks/{taskId}/dev-submit
     * body: { "title": "...", "url": "...", "repoUrl": "...", "description": "..." }
     */
    @PostMapping("/tasks/{taskId}/dev-submit")
    public Map<String, Object> submitDev(@PathVariable Long taskId,
                                          @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String title = (String) body.get("title");
        String url = (String) body.get("url");
        String repoUrl = (String) body.get("repoUrl");
        String description = (String) body.get("description");

        if (title == null || title.trim().isEmpty() || url == null || url.trim().isEmpty()
                || repoUrl == null || repoUrl.trim().isEmpty()) {
            return error("title、url 和 repoUrl 不能为空");
        }

        log.info("提交开发作品，mis: {}, taskId: {}", mis, taskId);
        bountyService.submitDev(taskId, mis, title.trim(), url.trim(),
                repoUrl.trim(),
                description != null ? description.trim() : "");
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 投票（设计阶段，每人最多3票，同一方案只能投1票）
     * POST /ulivepde/api/bounty/tasks/{taskId}/vote
     * body: { "submissionId": 123 }
     */
    @PostMapping("/tasks/{taskId}/vote")
    public Map<String, Object> vote(@PathVariable Long taskId,
                                     @RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        Long submissionId = body.get("submissionId") != null
                ? ((Number) body.get("submissionId")).longValue() : null;
        if (submissionId == null) return error("submissionId 不能为空");

        log.info("投票，mis: {}, taskId: {}, submissionId: {}", mis, taskId, submissionId);
        BountyService.VoteResult r = bountyService.vote(taskId, submissionId, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("submissionId", r.getSubmissionId());
        result.put("voteCount", r.getVoteCount());
        result.put("myVoteCount", r.getMyVoteCount());
        return result;
    }

    /**
     * 取消投票
     * DELETE /ulivepde/api/bounty/tasks/{taskId}/vote/{submissionId}
     */
    @DeleteMapping("/tasks/{taskId}/vote/{submissionId}")
    public Map<String, Object> unvote(@PathVariable Long taskId,
                                       @PathVariable Long submissionId,
                                       HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("取消投票，mis: {}, taskId: {}, submissionId: {}", mis, taskId, submissionId);
        BountyService.VoteResult r = bountyService.unvote(taskId, submissionId, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("submissionId", r.getSubmissionId());
        result.put("voteCount", r.getVoteCount());
        result.put("myVoteCount", r.getMyVoteCount());
        return result;
    }

    /**
     * 加入共建
     * POST /ulivepde/api/bounty/join
     * body: { "dept": "...", "role": "pm/design/fe/be/qa/other", "customRole": "...", "reason": "..." }
     */
    @PostMapping("/join")
    public Map<String, Object> join(@RequestBody Map<String, Object> body,
                                     HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String dept = (String) body.get("dept");
        String role = (String) body.get("role");
        String customRole = (String) body.get("customRole");
        String reason = (String) body.get("reason");

        if (role == null || role.trim().isEmpty()) {
            return error("请选择角色");
        }
        if (dept == null || dept.trim().isEmpty()) {
            return error("请填写部门");
        }
        if ("other".equals(role) && (customRole == null || customRole.trim().isEmpty())) {
            return error("请填写你的角色");
        }

        // 从 SSO 获取用户姓名
        String userName = "";
        try {
            SsoUserInfo userInfo = ssoService.getUserInfo(mis);
            if (userInfo != null && userInfo.getName() != null) {
                userName = userInfo.getName();
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败，mis: {}", mis);
        }

        log.info("加入共建，mis: {}, role: {}", mis, role);
        bountyService.joinBounty(mis, userName, dept.trim(), role.trim(),
                customRole != null ? customRole.trim() : "",
                reason != null ? reason.trim() : "");

        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 查询当前用户是否已加入共建
     * GET /ulivepde/api/bounty/membership
     * 返回: { joined: true/false, rank: 第几个加入, total: 总人数 }
     */
    @GetMapping("/membership")
    public Map<String, Object> membership(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        boolean joined = bountyService.isMember(mis);
        Map<String, Object> result = new HashMap<>();
        result.put("joined", joined);
        result.put("total", bountyService.getMemberCount());
        if (joined) {
            result.put("rank", bountyService.getMemberRank(mis));
        }
        return result;
    }

    /**
     * 查询所有共建成员列表
     * GET /ulivepde/api/bounty/members
     * 返回: { members: [{ mis, name, dept, role, customRole, reason, joinedAt }] }
     */
    @GetMapping("/members")
    public Map<String, Object> listMembers(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("查询共建成员列表，mis: {}", mis);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> members = new java.util.ArrayList<>();
        for (BountyMember m : bountyService.getAllMembers()) {
            Map<String, Object> item = new HashMap<>();
            item.put("mis", m.getUserMis());
            item.put("name", m.getUserName());
            item.put("dept", m.getDept());
            item.put("role", m.getRole());
            item.put("customRole", m.getCustomRole());
            item.put("reason", m.getReason());
            item.put("joinedAt", m.getAddTime() != null ? sdf.format(m.getAddTime()) : null);
            members.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("members", members);
        return result;
    }

    /**
     * 相似标题检测（轻量提示，不阻断提交）
     * GET /ulivepde/api/bounty/ideas/similar?title=xxx&excludeId=123
     */
    @GetMapping("/ideas/similar")
    public Map<String, Object> findSimilarIdeas(@RequestParam String title,
                                                 @RequestParam(required = false) Long excludeId) {
        List<Map<String, Object>> similars = new java.util.ArrayList<>();
        for (BountyTask task : bountyService.findSimilarIdeas(title, excludeId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("createdBy", task.getCreatedBy());
            similars.add(item);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("similars", similars);
        return result;
    }

    /**
     * 用户发布 Idea
     * POST /ulivepde/api/bounty/ideas
     * body: { "title": "...", "description": "...", "coverUrl": "...", "refLink": "..." }
     */
    @PostMapping("/ideas")
    public Map<String, Object> createIdea(@RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String coverUrl = (String) body.get("coverUrl");
        String refLink = (String) body.get("refLink");

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return error("title 和 description 不能为空");
        }

        log.info("发布Idea，mis: {}", mis);
        bountyService.createIdea(title.trim(), description.trim(), coverUrl, refLink, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 编辑自己的 Idea
     * PUT /ulivepde/api/bounty/ideas/{id}
     */
    @PutMapping("/ideas/{id}")
    public Map<String, Object> updateIdea(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String coverUrl = (String) body.get("coverUrl");
        String refLink = (String) body.get("refLink");

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return error("title 和 description 不能为空");
        }

        try {
            bountyService.updateIdea(id, title.trim(), description.trim(), coverUrl, refLink, mis);
            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            return result;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 删除自己的 Idea（软删除，仅本人可操作）
     * DELETE /ulivepde/api/bounty/ideas/{id}
     */
    @DeleteMapping("/ideas/{id}")
    public Map<String, Object> deleteIdea(@PathVariable Long id, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        try {
            bountyService.deleteIdea(id, mis);
            Map<String, Object> result = new HashMap<>();
            result.put("ok", true);
            return result;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    /**
     * 点赞/取消点赞
     * POST /ulivepde/api/bounty/tasks/{id}/like
     */
    @PostMapping("/tasks/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        boolean liked = bountyService.toggleLike(id, mis);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        result.put("liked", liked);
        return result;
    }

    // ---- 管理员接口 ----

    /**
     * 发布官方任务（管理员）
     * POST /ulivepde/api/bounty/admin/tasks
     */
    @PostMapping("/admin/tasks")
    public Map<String, Object> createTask(@RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            return error("无管理员权限");
        }

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        String status = (String) body.get("status");
        Long devMinStage = body.get("devMinStage") != null ? ((Number) body.get("devMinStage")).longValue() : 5L;
        String rewardDesc = (String) body.get("rewardDesc");

        if (title == null || title.trim().isEmpty()) {
            return error("title 不能为空");
        }

        Date designDeadline = parseDate((String) body.get("designDeadline"));
        Date votingEnd = parseDate((String) body.get("votingEnd"));
        Date devDeadline = parseDate((String) body.get("devDeadline"));

        log.info("发布官方任务，adminMis: {}, title: {}", mis, title);
        bountyService.createTask(title.trim(), description, "official", status,
                mis, designDeadline, votingEnd, devDeadline, devMinStage, rewardDesc);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 推进任务状态（管理员）
     * PUT /ulivepde/api/bounty/admin/tasks/{taskId}/status
     * body: { "status": "design_voting" }
     */
    @PutMapping("/admin/tasks/{taskId}/status")
    public Map<String, Object> updateTaskStatus(@PathVariable Long taskId,
                                                 @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            return error("无管理员权限");
        }

        String status = (String) body.get("status");
        if (status == null || status.trim().isEmpty()) {
            return error("status 不能为空");
        }

        log.info("更新任务状态，adminMis: {}, taskId: {}, status: {}", mis, taskId, status);
        bountyService.updateTaskStatus(taskId, status.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 编辑任务（管理员）
     * PUT /ulivepde/api/bounty/admin/tasks/{taskId}
     */
    @PutMapping("/admin/tasks/{taskId}")
    public Map<String, Object> updateTask(@PathVariable Long taskId,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) return error("无管理员权限");

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        Long devMinStage = body.get("devMinStage") != null ? ((Number) body.get("devMinStage")).longValue() : 5L;
        String rewardDesc = (String) body.get("rewardDesc");

        if (title == null || title.trim().isEmpty()) return error("title 不能为空");

        Date designDeadline = parseDate((String) body.get("designDeadline"));
        Date votingEnd      = parseDate((String) body.get("votingEnd"));
        Date devDeadline    = parseDate((String) body.get("devDeadline"));

        log.info("编辑任务，adminMis: {}, taskId: {}", mis, taskId);
        bountyService.updateTask(taskId, title.trim(), description, designDeadline, votingEnd, devDeadline, devMinStage, rewardDesc);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 删除任务（管理员，软删除）
     * DELETE /ulivepde/api/bounty/admin/tasks/{taskId}
     */
    @DeleteMapping("/admin/tasks/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable Long taskId,
                                           HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) return error("无管理员权限");

        log.info("删除任务，adminMis: {}, taskId: {}", mis, taskId);
        bountyService.deleteTask(taskId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 翻牌 Idea（管理员）
     * POST /ulivepde/api/bounty/admin/tasks/{taskId}/feature
     */
    @PostMapping("/admin/tasks/{taskId}/feature")
    public Map<String, Object> featureIdea(@PathVariable Long taskId,
                                            @RequestBody(required = false) Map<String, Object> body,
                                            HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            return error("无管理员权限");
        }

        String reason = body != null ? (String) body.get("reason") : null;
        log.info("翻牌Idea，adminMis: {}, taskId: {}, reason: {}", mis, taskId, reason);
        bountyService.featureIdea(taskId, mis, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 审核提交（管理员）
     * POST /ulivepde/api/bounty/admin/submissions/{submissionId}/review
     * body: { "status": "approved", "rejectReason": "..." }
     */
    @PostMapping("/admin/submissions/{submissionId}/review")
    public Map<String, Object> reviewSubmission(@PathVariable Long submissionId,
                                                 @RequestBody Map<String, Object> body,
                                                 HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            return error("无管理员权限");
        }

        String status = (String) body.get("status");
        String rejectReason = (String) body.get("rejectReason");

        if (status == null || status.trim().isEmpty()) {
            return error("status 不能为空");
        }

        log.info("审核提交，adminMis: {}, submissionId: {}, status: {}", mis, submissionId, status);
        bountyService.reviewSubmission(submissionId, status.trim(), rejectReason);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    /**
     * 设置 winner（管理员）
     * POST /ulivepde/api/bounty/admin/submissions/{submissionId}/winner
     */
    @PostMapping("/admin/submissions/{submissionId}/winner")
    public Map<String, Object> setWinner(@PathVariable Long submissionId,
                                          HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            return error("无管理员权限");
        }

        log.info("设置winner，adminMis: {}, submissionId: {}", mis, submissionId);
        bountyService.setWinner(submissionId);
        Map<String, Object> result = new HashMap<>();
        result.put("ok", true);
        return result;
    }

    // ---- 工具方法 ----

    private Map<String, Object> error(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("ok", false);
        err.put("error", msg);
        return err;
    }

    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
}
