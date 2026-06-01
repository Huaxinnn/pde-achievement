package com.meituan.pde.controller;

import com.meituan.pde.common.ForbiddenException;
import com.meituan.pde.service.AdminService;
import com.meituan.pde.service.OrgInfoRefreshService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private OrgInfoRefreshService orgInfoRefreshService;

    /**
     * 查询当前用户是否有管理员权限，任何登录用户均可调用，不返回 403。
     */
    @GetMapping("/me")
    public Map<String, Object> getAdminMe(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        Map<String, Object> result = new HashMap<>();
        result.put("isAdmin", adminService.isAdmin(mis));
        return result;
    }

    /**
     * 获取审核列表，仅管理员可访问。
     */
    @GetMapping("/reviews")
    public Map<String, Object> listReviews(
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            throw new ForbiddenException("无管理员权限");
        }
        return adminService.listReviews(status, page, pageSize);
    }

    /**
     * 执行审核操作（通过 / 不通过），仅管理员可访问。
     */
    @PostMapping("/reviews/{id}/review")
    public Map<String, Object> doReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            throw new ForbiddenException("无管理员权限");
        }
        String action = (String) body.get("action");
        String rejectReason = (String) body.getOrDefault("rejectReason", "");
        if (!"pass".equals(action) && !"reject".equals(action)) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "action must be 'pass' or 'reject'");
            return err;
        }
        log.info("[Admin] mis={} action={} id={}", mis, action, id);
        return adminService.doReview(id, action, rejectReason, mis);
    }

    /**
     * 一次性回填历史记录的 user_org 字段，仅管理员可访问。
     * 每次处理 batchSize 个 mis，反复调用直到 done=true。
     */
    @PostMapping("/backfill/user-org")
    public Map<String, Object> backfillUserOrg(
            @RequestParam(defaultValue = "100") int batchSize,
            HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            throw new ForbiddenException("无管理员权限");
        }
        log.info("[Admin] backfillUserOrg mis={} batchSize={}", mis, batchSize);
        return adminService.backfillUserOrg(batchSize);
    }

    /**
     * 手动触发一次 org_info 员工人数刷新，仅管理员可访问。
     */
    @PostMapping("/refresh/org-info")
    public Map<String, Object> refreshOrgInfo(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        if (!adminService.isAdmin(mis)) {
            throw new ForbiddenException("无管理员权限");
        }
        log.info("[Admin] refreshOrgInfo triggered by mis={}", mis);
        orgInfoRefreshService.refresh();
        Map<String, Object> result = new HashMap<>();
        result.put("done", true);
        return result;
    }
}
