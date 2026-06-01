package com.meituan.pde.service;

import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.config.AdminConfig;
import com.meituan.pde.dao.StageDao;
import com.meituan.pde.dao.UserStageProgressDao;
import com.meituan.pde.dao.UserStepProgressDao;
import com.meituan.pde.entity.Stage;
import com.meituan.pde.entity.UserStageProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminService {

    private static final Date DEFAULT_PASSED_AT;
    static {
        try {
            DEFAULT_PASSED_AT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse("1970-01-01 00:00:00");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private UserStageProgressDao userStageProgressDao;

    @Autowired
    private UserStepProgressDao userStepProgressDao;

    @Autowired
    private StageDao stageDao;

    @Autowired
    private SsoService ssoService;

    @Autowired
    private ActivityBroadcastService activityBroadcastService;

    public Map<String, Object> backfillUserOrg(int batchSize) {
        if (batchSize < 1 || batchSize > 500) batchSize = 100;

        // 合并两种待回填情况：user_org 为空 或 user_org_id 为空，合并后截断到 batchSize
        java.util.Set<String> misSet = new java.util.LinkedHashSet<>();
        misSet.addAll(userStageProgressDao.findMisWithEmptyOrg(batchSize));
        misSet.addAll(userStepProgressDao.findMisWithEmptyOrg(batchSize));
        misSet.addAll(userStageProgressDao.findMisWithEmptyOrgId(batchSize));
        misSet.addAll(userStepProgressDao.findMisWithEmptyOrgId(batchSize));
        java.util.List<String> allMis = new java.util.ArrayList<>(misSet);
        if (allMis.size() > batchSize) {
            allMis = allMis.subList(0, batchSize);
        }

        int updatedStage = 0;
        int updatedStep = 0;
        for (String mis : allMis) {
            String orgPath = ssoService.getFullOrgPathForBackfill(mis);
            // SDK 异常时返回空字符串，跳过本次，下次重试
            if (!StringUtils.hasText(orgPath)) continue;
            // "已离职" 也正常写入，让 org 字段有值，countEmptyOrg 才会减少
            SsoUserInfo userInfo = ssoService.getUserInfo(mis);
            String orgId = userInfo.getOrgId() != null ? userInfo.getOrgId() : "";
            updatedStage += userStageProgressDao.updateOrgByMis(mis, orgPath);
            updatedStep += userStepProgressDao.updateOrgByMis(mis, orgPath);
            if (StringUtils.hasText(orgId)) {
                userStageProgressDao.updateOrgIdByMis(mis, orgId);
                userStepProgressDao.updateOrgIdByMis(mis, orgId);
            }
        }

        int remainingStage = userStageProgressDao.countEmptyOrg();
        int remainingStep = userStepProgressDao.countEmptyOrg();

        Map<String, Object> result = new HashMap<>();
        result.put("processedMis", allMis.size());
        result.put("updatedStageRows", updatedStage);
        result.put("updatedStepRows", updatedStep);
        result.put("remainingStage", remainingStage);
        result.put("remainingStep", remainingStep);
        result.put("done", remainingStage == 0 && remainingStep == 0);
        return result;
    }

    public boolean isAdmin(String mis) {
        String raw = AdminConfig.ADMIN_MIS_LIST;
        if (!StringUtils.hasText(raw) || !StringUtils.hasText(mis)) {
            return false;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .collect(Collectors.toList())
                .contains(mis.trim());
    }

    public Map<String, Object> listReviews(String status, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;
        int offset = (page - 1) * pageSize;
        String queryStatus = "all".equals(status) ? null : status;

        List<UserStageProgress> records = userStageProgressDao.findPendingManual(queryStatus, offset, pageSize);
        int total = userStageProgressDao.countPendingManual(queryStatus);

        // 批量拉用户信息，避免 N+1（去重，同一用户可能提交多关）
        List<String> misList = records.stream().map(UserStageProgress::getUserMis).distinct().collect(Collectors.toList());
        Map<String, SsoUserInfo> userInfoMap = ssoService.batchGetUserInfo(misList);

        // Stage 只有 7 条，全量加载建 map
        Map<Long, Stage> stageMap = stageDao.findAll().stream()
                .collect(Collectors.toMap(Stage::getId, s -> s));

        List<Map<String, Object>> list = new ArrayList<>();
        for (UserStageProgress p : records) {
            Stage stage = stageMap.get(p.getStageId());
            SsoUserInfo userInfo = userInfoMap.getOrDefault(p.getUserMis(),
                    buildFallbackUserInfo(p.getUserMis()));
            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("mis", p.getUserMis());
            item.put("name", userInfo.getName());
            item.put("org", p.getUserOrg() != null && !p.getUserOrg().isEmpty() ? p.getUserOrg() : userInfo.getOrg());
            item.put("stageId", p.getStageId());
            item.put("stageName", stage != null ? stage.getName() : "");
            item.put("stageTitle", stage != null ? stage.getTitle() : "");
            item.put("lane", p.getLane());
            item.put("url", p.getUrl());
            item.put("submittedAt", p.getCreateTime());
            item.put("status", p.getVerifyStatus());
            item.put("reviewedAt", p.getReviewedAt());
            item.put("reviewedBy", p.getReviewedBy());
            item.put("rejectReason", p.getRejectReason());
            list.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("list", list);
        return result;
    }

    @Transactional
    public Map<String, Object> doReview(Long id, String action, String rejectReason, String reviewerMis) {
        UserStageProgress progress = userStageProgressDao.findById(id);
        if (progress == null) {
            throw new IllegalArgumentException("Record not found: " + id);
        }

        Date now = new Date();
        if ("pass".equals(action)) {
            progress.setVerifyStatus("passed");
            progress.setPassedAt(now);
        } else {
            progress.setVerifyStatus("failed");
            progress.setPassedAt(DEFAULT_PASSED_AT);
        }
        progress.setReviewedAt(now);
        progress.setReviewedBy(reviewerMis);
        progress.setRejectReason(rejectReason);
        SsoUserInfo revieweeInfo = ssoService.getUserInfo(progress.getUserMis());
        if (StringUtils.hasText(revieweeInfo.getOrg())) {
            progress.setUserOrg(revieweeInfo.getOrg());
        }
        if (StringUtils.hasText(revieweeInfo.getOrgId())) {
            progress.setUserOrgId(revieweeInfo.getOrgId());
        }
        userStageProgressDao.updateReview(progress);

        if ("pass".equals(action)) {
            broadcastComplete(progress.getUserMis(), progress.getStageId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        result.put("action", action);
        return result;
    }

    private SsoUserInfo buildFallbackUserInfo(String mis) {
        SsoUserInfo info = new SsoUserInfo();
        info.setMis(mis);
        info.setName(mis);
        info.setOrg("");
        return info;
    }

    private void broadcastComplete(String userMis, Long stageId) {
        try {
            Stage stage = stageDao.findById(stageId);
            if (stage == null) return;
            SsoUserInfo userInfo = ssoService.getUserInfo(userMis);
            com.meituan.pde.common.ActivityEvent event = new com.meituan.pde.common.ActivityEvent(
                    userMis, userInfo.getName(), "complete", stageId.intValue(), stage.getName());
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            activityBroadcastService.broadcast(event);
                        }
                    });
            } else {
                activityBroadcastService.broadcast(event);
            }
        } catch (Exception e) {
            log.error("[AdminService] broadcastComplete 异常 mis={} stageId={}", userMis, stageId, e);
        }
    }

}
