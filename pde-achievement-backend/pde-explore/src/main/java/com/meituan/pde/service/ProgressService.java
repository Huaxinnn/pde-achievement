package com.meituan.pde.service;

import com.meituan.pde.common.ActivityEvent;
import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.dao.OrgInfoDao;
import com.meituan.pde.dao.StageDao;
import com.meituan.pde.dao.StageStepDao;
import com.meituan.pde.dao.UserStageProgressDao;
import com.meituan.pde.dao.UserStepProgressDao;
import com.meituan.pde.dao.VerifyCheckinLogDao;
import com.meituan.pde.entity.OrgInfo;
import com.meituan.pde.entity.Stage;
import com.meituan.pde.entity.UserStageProgress;
import com.meituan.pde.entity.UserStepProgress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service
public class ProgressService {

    @Autowired
    private UserStageProgressDao userStageProgressDao;

    @Autowired
    private UserStepProgressDao userStepProgressDao;

    @Autowired
    private StageStepDao stageStepDao;

    @Autowired
    private StageDao stageDao;

    @Autowired
    private SsoService ssoService;

    @Autowired
    private ActivityBroadcastService activityBroadcastService;

    @Autowired
    private VerifyCheckinLogDao verifyCheckinLogDao;

    @Autowired
    private OrgInfoDao orgInfoDao;

    /**
     * 标记步骤完成（幂等）
     */
    @Transactional
    public StepCompleteResponse markStepComplete(String userMis, Long stageId, Long stepId) {
        // 幂等写入步骤进度（先查后插，兼容 SQLite 和 MySQL）
        boolean isNewStep = userStepProgressDao.existsByUserMisAndStepId(userMis, stepId) == 0;
        // 插入前先判断该关卡是否已有任何步骤记录，用于决定是否播报"开始挑战"
        int countBefore = userStepProgressDao.countByUserMisAndStageId(userMis, stageId);
        boolean isFirstStepOfStage = isNewStep && countBefore == 0;
        log.info("[播报调试] mis={} stageId={} stepId={} isNewStep={} countBefore={} isFirstStepOfStage={}",
                userMis, stageId, stepId, isNewStep, countBefore, isFirstStepOfStage);
        // 只有真正新步骤才查 org（幂等重复提交时不打 RPC）
        SsoUserInfo orgInfo = isNewStep ? ssoService.getUserInfo(userMis) : null;
        String userOrg = orgInfo != null ? orgInfo.getOrg() : "";
        String userOrgId = orgInfo != null ? (orgInfo.getOrgId() != null ? orgInfo.getOrgId() : "") : "";
        if (isNewStep) {
            UserStepProgress stepProgress = new UserStepProgress();
            stepProgress.setUserMis(userMis);
            stepProgress.setUserOrg(userOrg);
            stepProgress.setUserOrgId(userOrgId);
            stepProgress.setStepId(stepId);
            userStepProgressDao.insertIgnore(stepProgress);
        }

        // 该关卡首个步骤完成 → 播报"开始挑战"
        if (isFirstStepOfStage) {
            log.info("[播报调试] 触发 broadcastStartEvent mis={} stageId={}", userMis, stageId);
            broadcastStartEvent(userMis, stageId);
        }

        // 查该关卡步骤总数 vs 已完成数
        int totalSteps = stageStepDao.findByStageId(stageId).size();
        int completedSteps = userStepProgressDao.countByUserMisAndStageId(userMis, stageId);

        boolean stageCompleted = false;
        if (completedSteps >= totalSteps && totalSteps > 0) {
            Stage stage = stageDao.findById(stageId);
            if (stage != null && "self".equals(stage.getVerifyType())) {
                markStagePassed(userMis, stageId, null, null, null, userOrg, userOrgId);
                stageCompleted = true;
            }
        }

        StepCompleteResponse response = new StepCompleteResponse();
        response.setOk(true);
        response.setStageCompleted(stageCompleted);
        return response;
    }

    /**
     * 提交通关验证
     */
    @Transactional
    public VerifyResponse submitVerify(String userMis, Long stageId, String value, String lane, String url) {
        SsoUserInfo orgInfo = ssoService.getUserInfo(userMis);
        String userOrg = orgInfo.getOrg() != null ? orgInfo.getOrg() : "";
        String userOrgId = orgInfo.getOrgId() != null ? orgInfo.getOrgId() : "";
        Stage stage = stageDao.findById(stageId);
        if (stage == null) {
            throw new IllegalArgumentException("Stage not found: " + stageId);
        }

        String verifyType = stage.getVerifyType();
        String verifyStatus;
        String message;
        boolean stageCompleted = false;

        if ("curl".equals(verifyType)) {
            // 查询用户最近 10 分钟内是否有 checkin 上报记录（时间在 Java 侧计算，兼容 SQLite/MySQL）
            Date minTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000L);
            int checkinCount = verifyCheckinLogDao.countCheckinAfter(userMis, stageId.intValue(), minTime);
            if (checkinCount > 0) {
                verifyStatus = "passed";
                message = "校验通过！";
                stageCompleted = true;
                markStagePassed(userMis, stageId, "curl-verified", null, null, userOrg, userOrgId);
            } else {
                verifyStatus = "failed";
                message = "未检测到命令执行记录，请确认已在终端运行命令（需连接美团 VPN）";
            }
        } else if ("url".equals(verifyType)) {
            if (!StringUtils.hasText(value)) {
                VerifyResponse r = new VerifyResponse();
                r.setVerifyStatus("pending");
                r.setMessage("URL 不能为空，已提交人工审核");
                r.setStageCompleted(false);
                saveOrUpdateProgress(userMis, stageId, "pending", value != null ? value : "", null, lane, url, userOrg, userOrgId);
                return r;
            }
            boolean accessible = checkUrlAccessible(value);
            if (accessible) {
                verifyStatus = "passed";
                message = "验证通过！";
                stageCompleted = true;
                markStagePassed(userMis, stageId, value, lane, url, userOrg, userOrgId);
            } else {
                verifyStatus = "pending";
                message = "URL 暂时无法访问，已提交人工审核";
                saveOrUpdateProgress(userMis, stageId, verifyStatus, value, null, lane, url, userOrg, userOrgId);
            }
        } else if ("paste".equals(verifyType) || "clipboard".equals(verifyType)) {
            // 前端已完成格式校验，后端直接通关
            verifyStatus = "passed";
            message = "验证通过！";
            stageCompleted = true;
            markStagePassed(userMis, stageId, value, lane, url, userOrg, userOrgId);
        } else {
            // manual：人工审核
            verifyStatus = "pending";
            message = "已提交，运营团队将在 1-3 个工作日内完成审核";
            saveOrUpdateProgress(userMis, stageId, verifyStatus, value, null, lane, url, userOrg, userOrgId);
        }

        VerifyResponse response = new VerifyResponse();
        response.setVerifyStatus(verifyStatus);
        response.setMessage(message);
        response.setStageCompleted(stageCompleted);
        return response;
    }

    /**
     * 获取用户当前最高已通关关卡 ID
     */
    public Long getMaxPassedStageId(String userMis) {
        return userStageProgressDao.findMaxPassedStageIdByUserMis(userMis);
    }

    /**
     * 获取用户已通关关卡数
     */
    public int getPassedStageCount(String userMis) {
        return (int) userStageProgressDao.findByUserMis(userMis).stream()
                .filter(p -> "passed".equals(p.getVerifyStatus()))
                .count();
    }

    /**
     * 获取用户在某关卡的通关记录
     */
    public UserStageProgress getStageProgress(String userMis, Long stageId) {
        return userStageProgressDao.findByUserMisAndStageId(userMis, stageId);
    }

    private void markStagePassed(String userMis, Long stageId, String submittedValue, String lane, String url, String userOrg, String userOrgId) {
        saveOrUpdateProgress(userMis, stageId, "passed",
                submittedValue != null ? submittedValue : "", new Date(), lane, url, userOrg, userOrgId);
        broadcastCompleteEvent(userMis, stageId);
    }

    private void broadcastStartEvent(String userMis, Long stageId) {
        try {
            Stage stage = stageDao.findById(stageId);
            if (stage == null) {
                log.warn("[播报调试] broadcastStartEvent stage 为空 stageId={}", stageId);
                return;
            }
            SsoUserInfo userInfo = ssoService.getUserInfo(userMis);
            log.info("[播报调试] broadcastStartEvent 构建事件 name={} stageTitle={}", userInfo.getName(), stage.getName());
            ActivityEvent event = new ActivityEvent(userMis, userInfo.getName(), "start", stageId.intValue(), stage.getName());
            broadcastAfterCommit(event);
        } catch (Exception e) {
            log.error("[播报调试] broadcastStartEvent 异常", e);
        }
    }

    private void broadcastCompleteEvent(String userMis, Long stageId) {
        try {
            Stage stage = stageDao.findById(stageId);
            if (stage == null) return;
            SsoUserInfo userInfo = ssoService.getUserInfo(userMis);
            ActivityEvent event = new ActivityEvent(userMis, userInfo.getName(), "complete", stageId.intValue(), stage.getName());
            broadcastAfterCommit(event);
        } catch (Exception e) {
            // 播报失败不影响主流程
        }
    }

    private void broadcastAfterCommit(ActivityEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    activityBroadcastService.broadcast(event);
                }
            });
        } else {
            activityBroadcastService.broadcast(event);
        }
    }

    private static final java.util.Date DEFAULT_PASSED_AT;
    static {
        try {
            DEFAULT_PASSED_AT = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse("1970-01-01 00:00:00");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void saveOrUpdateProgress(String userMis, Long stageId, String verifyStatus,
                                       String submittedValue, Date passedAt, String lane, String url,
                                       String userOrg, String userOrgId) {
        UserStageProgress existing = userStageProgressDao.findByUserMisAndStageId(userMis, stageId);
        Date effectivePassedAt = passedAt != null ? passedAt : DEFAULT_PASSED_AT;
        if (existing == null) {
            UserStageProgress progress = new UserStageProgress();
            progress.setUserMis(userMis);
            progress.setUserOrg(userOrg);
            progress.setUserOrgId(userOrgId != null ? userOrgId : "");
            progress.setStageId(stageId);
            progress.setVerifyStatus(verifyStatus);
            progress.setSubmittedValue(submittedValue);
            progress.setPassedAt(effectivePassedAt);
            progress.setLane(lane != null ? lane : "");
            progress.setUrl(url != null ? url : "");
            userStageProgressDao.insert(progress);
        } else {
            if (StringUtils.hasText(userOrg)) {
                existing.setUserOrg(userOrg);
            }
            if (StringUtils.hasText(userOrgId)) {
                existing.setUserOrgId(userOrgId);
            }
            existing.setVerifyStatus(verifyStatus);
            existing.setSubmittedValue(submittedValue);
            existing.setPassedAt(effectivePassedAt);
            existing.setLane(lane != null ? lane : "");
            existing.setUrl(url != null ? url : "");
            userStageProgressDao.update(existing);
        }
        // 顺带维护 org_info 的 org_name（emp_count 由定时任务刷新）
        if (StringUtils.hasText(userOrgId) && StringUtils.hasText(userOrg)) {
            try {
                OrgInfo orgInfo = new OrgInfo();
                orgInfo.setOrgId(userOrgId);
                orgInfo.setOrgName(userOrg);
                orgInfo.setEmpCount(0L);
                orgInfoDao.upsert(orgInfo);
            } catch (Exception e) {
                log.warn("[ProgressService] upsert org_info 失败 orgId={}", userOrgId, e);
            }
        }
    }

    private static final String ALLOWED_HOST_SUFFIX = ".awp.sankuai.com";

    private boolean checkUrlAccessible(String urlStr) {
        try {
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                log.warn("SSRF拦截：不允许的协议 {}", protocol);
                return false;
            }
            String host = url.getHost();
            if (!host.endsWith(ALLOWED_HOST_SUFFIX)) {
                log.warn("SSRF拦截：域名不在白名单 host={}", host);
                return false;
            }
            InetAddress addr = InetAddress.getByName(host);
            if (isPrivateOrReservedAddress(addr)) {
                log.warn("SSRF拦截：内网地址被拒绝 host={} ip={}", host, addr.getHostAddress());
                return false;
            }
            // 用已解析的 IP 发起连接，避免 DNS 重绑定（TOCTOU）
            int port = url.getPort() != -1 ? url.getPort() : ("https".equals(protocol) ? 443 : 80);
            URL ipUrl = new URL(protocol, addr.getHostAddress(), port, url.getFile());
            HttpURLConnection conn = (HttpURLConnection) ipUrl.openConnection();
            conn.setRequestProperty("Host", host);
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(false);
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPrivateOrReservedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int b0 = b[0] & 0xFF;
            int b1 = b[1] & 0xFF;
            // 100.64.0.0/10 (运营商 NAT / 美团内网常用段)
            if (b0 == 100 && (b1 & 0xC0) == 64) return true;
            // 169.254.0.0/16 (link-local，已由 isLinkLocalAddress 覆盖，双保险)
            if (b0 == 169 && b1 == 254) return true;
        }
        return false;
    }

    public static class StepCompleteResponse {
        private Boolean ok;
        private Boolean stageCompleted;

        public Boolean getOk() { return ok; }
        public void setOk(Boolean ok) { this.ok = ok; }
        public Boolean getStageCompleted() { return stageCompleted; }
        public void setStageCompleted(Boolean stageCompleted) { this.stageCompleted = stageCompleted; }
    }

    public static class VerifyResponse {
        private String verifyStatus;
        private String message;
        private Boolean stageCompleted;

        public String getVerifyStatus() { return verifyStatus; }
        public void setVerifyStatus(String verifyStatus) { this.verifyStatus = verifyStatus; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Boolean getStageCompleted() { return stageCompleted; }
        public void setStageCompleted(Boolean stageCompleted) { this.stageCompleted = stageCompleted; }
    }
}
