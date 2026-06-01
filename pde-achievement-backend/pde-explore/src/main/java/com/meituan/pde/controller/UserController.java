package com.meituan.pde.controller;

import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.dao.StageDao;
import com.meituan.pde.dao.UserStageProgressDao;
import com.meituan.pde.dao.UserStepProgressDao;
import com.meituan.pde.entity.Stage;
import com.meituan.pde.entity.UserStageProgress;
import com.meituan.pde.entity.UserStepProgress;
import com.meituan.pde.service.LeaderboardService;
import com.meituan.pde.service.SsoService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/user")
public class UserController {

    private static final Long REFLECTION_STAGE_ID = 0L;
    private static final String DEFAULT_REFLECTION_TEXT = "每次卡关都让我更像一名真正的工程师。";
    private static final Date DEFAULT_PASSED_AT = parseDefaultPassedAt();

    /** 固定东八区，避免依赖 JVM 系统时区 */
    private static final ZoneId ZONE_CST = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZONE_CST);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZONE_CST);

    // ---------------------------------------------------------------
    // 轻量本地缓存：对排行榜结果缓存 5 分钟，key = userMis
    // 避免每次访问成就报告都触发全量排行榜计算（findAllPassed + batchSSO）
    // ---------------------------------------------------------------
    private static final long LEADERBOARD_CACHE_TTL_MS = 5 * 60 * 1000L;

    private static class CachedLeaderboard {
        final LeaderboardService.LeaderboardResponse data;
        final long expireAt;

        CachedLeaderboard(LeaderboardService.LeaderboardResponse data) {
            this.data = data;
            this.expireAt = System.currentTimeMillis() + LEADERBOARD_CACHE_TTL_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }
    }

    private final ConcurrentHashMap<String, CachedLeaderboard> leaderboardCache = new ConcurrentHashMap<>();

    @Autowired
    private SsoService ssoService;

    @Autowired
    private UserStageProgressDao userStageProgressDao;

    @Autowired
    private UserStepProgressDao userStepProgressDao;

    @Autowired
    private StageDao stageDao;

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取用户信息，mis: {}", mis);
        try {
            SsoUserInfo userInfo = ssoService.getUserInfo(mis);
            Map<String, Object> response = new HashMap<>();
            response.put("mis", userInfo.getMis());
            response.put("name", userInfo.getName());
            response.put("avatar", userInfo.getAvatar());
            String fullOrg = userInfo.getFullOrgPath();
            response.put("org", StringUtils.hasText(fullOrg) ? fullOrg : userInfo.getOrg());
            Long maxStage = userStageProgressDao.findMaxPassedStageIdByUserMis(mis);
            response.put("completedCount", maxStage != null ? maxStage.intValue() : 0);
            log.info("获取用户信息成功，mis: {}", mis);
            return response;
        } catch (Exception e) {
            log.error("获取用户信息失败，mis: {}", mis, e);
            throw e;
        }
    }

    /**
     * 获取成就报告数据（聚合接口）
     */
    @GetMapping("/achievement-report")
    public Map<String, Object> getAchievementReport(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取成就报告，mis: {}", mis);

        SsoUserInfo userInfo = ssoService.getUserInfo(mis);
        List<Stage> stages = stageDao.findAll();
        List<UserStageProgress> stageProgressList = userStageProgressDao.findByUserMis(mis);

        // ---- 构建已通关关卡 map ----
        Map<Long, UserStageProgress> passedProgressByStage = new HashMap<>();
        for (UserStageProgress progress : stageProgressList) {
            if (progress == null || progress.getStageId() == null || progress.getStageId() <= 0) {
                continue;
            }
            if (!"passed".equals(progress.getVerifyStatus())) {
                continue;
            }
            if (!isValidPassedAt(progress.getPassedAt())) {
                continue;
            }
            passedProgressByStage.put(progress.getStageId(), progress);
        }

        int completedCount = passedProgressByStage.size();

        // ---- [FIX-P0] 一次性批量拉取该用户所有步骤记录，按 stageId 分组，避免 N+1 查询 ----
        List<UserStepProgress> allStepRecords = userStepProgressDao.findAllByUserMis(mis);
        Map<Long, List<UserStepProgress>> stepsByStage = allStepRecords.stream()
                .filter(s -> s.getStageId() != null)
                .collect(Collectors.groupingBy(UserStepProgress::getStageId));

        List<Long> levelDurationHours = new ArrayList<>();
        List<Map<String, Object>> levelTimeline = new ArrayList<>();
        List<Date> allStepTimes = new ArrayList<>();
        Date earliestStepTime = null;
        Date latestPassedTime = null;
        Long hardestLevel = null;
        long hardestHours = -1L;

        for (Stage stage : stages) {
            if (stage == null || stage.getId() == null) {
                continue;
            }
            Long stageId = stage.getId();
            // 直接从内存 map 取，不再发 DB 请求
            List<UserStepProgress> steps = stepsByStage.getOrDefault(stageId, Collections.emptyList());

            Date firstStepTime = null;
            for (UserStepProgress step : steps) {
                if (step == null || step.getCompletedAt() == null) {
                    continue;
                }
                Date completedAt = step.getCompletedAt();
                allStepTimes.add(completedAt);
                if (firstStepTime == null || completedAt.before(firstStepTime)) {
                    firstStepTime = completedAt;
                }
            }

            UserStageProgress stageProgress = passedProgressByStage.get(stageId);
            Date passedAt = stageProgress != null ? stageProgress.getPassedAt() : null;

            long durationHours = 0L;
            if (firstStepTime != null && passedAt != null && !passedAt.before(firstStepTime)) {
                durationHours = calcDurationHours(firstStepTime, passedAt);
            }

            if (firstStepTime != null && (earliestStepTime == null || firstStepTime.before(earliestStepTime))) {
                earliestStepTime = firstStepTime;
            }
            if (passedAt != null && (latestPassedTime == null || passedAt.after(latestPassedTime))) {
                latestPassedTime = passedAt;
            }

            levelDurationHours.add(durationHours);
            if (durationHours > hardestHours) {
                hardestHours = durationHours;
                hardestLevel = stageId;
            }

            Map<String, Object> timelineItem = new HashMap<>();
            timelineItem.put("stageId", stageId);
            timelineItem.put("stageName", stage.getName());
            timelineItem.put("stageTitle", stage.getTitle());
            timelineItem.put("passedAt", formatDateTime(passedAt));
            timelineItem.put("durationHours", durationHours);
            timelineItem.put("completed", passedAt != null);
            levelTimeline.add(timelineItem);
        }

        if (earliestStepTime == null && !passedProgressByStage.isEmpty()) {
            for (UserStageProgress progress : passedProgressByStage.values()) {
                if (progress.getPassedAt() == null) {
                    continue;
                }
                if (earliestStepTime == null || progress.getPassedAt().before(earliestStepTime)) {
                    earliestStepTime = progress.getPassedAt();
                }
            }
        }

        int activeDays = countActiveDays(allStepTimes);
        int maxContinuousDays = calcMaxContinuousDays(allStepTimes);
        Date latestStepTime = findLatestTime(allStepTimes);
        String lateSubmitTime = formatTime(latestStepTime);
        int lateSubmitHour = extractHour(latestStepTime);

        long totalDurationHours = 0L;
        if (earliestStepTime != null && latestPassedTime != null && !latestPassedTime.before(earliestStepTime)) {
            totalDurationHours = calcDurationHours(earliestStepTime, latestPassedTime);
        }
        String totalDurationText = formatDuration(totalDurationHours);
        int totalDays = totalDurationHours > 0 ? (int) (totalDurationHours / 24) : 0;

        // ---- [FIX-P0] 排行榜结果缓存 5 分钟，避免每次请求都做全量计算+批量SSO ----
        LeaderboardService.LeaderboardResponse groupLeaderboard = getCachedGroupLeaderboard(mis);
        int globalRank = safeInt(groupLeaderboard.getGlobalRank());
        int totalPassCount = safeInt(groupLeaderboard.getTotalPassCount());
        int rankPctRaw = safeInt(groupLeaderboard.getRankPct());
        int rankPct = globalRank > 0 && totalPassCount > 0 ? Math.max(rankPctRaw, 50) : rankPctRaw;

        LeaderboardService.GroupMeta groupMeta = groupLeaderboard.getGroup();
        Map<String, Object> group = new HashMap<>();
        group.put("name", groupMeta != null && groupMeta.getName() != null ? groupMeta.getName() : "");
        group.put("myRank", groupMeta != null ? safeInt(groupMeta.getMyRank()) : 0);
        group.put("totalMembers", groupMeta != null ? safeInt(groupMeta.getTotalMembers()) : 0);
        group.put("passCount", groupMeta != null ? safeInt(groupMeta.getPassCount()) : 0);

        List<Map<String, Object>> groupRankings = new ArrayList<>();
        List<LeaderboardService.LeaderboardEntry> entries = groupLeaderboard.getList();
        if (entries != null) {
            for (LeaderboardService.LeaderboardEntry entry : entries) {
                Map<String, Object> item = new HashMap<>();
                item.put("rank", safeInt(entry.getRank()));
                item.put("mis", entry.getMis());
                item.put("name", entry.getName());
                item.put("avatar", entry.getAvatar());
                item.put("org", entry.getOrg());
                item.put("completedCount", safeInt(entry.getCompletedCount()));
                item.put("stageName", entry.getStageName());
                item.put("lastPassedAt", formatDateTime(entry.getLastPassedAt()));
                groupRankings.add(item);
            }
        }

        String persona = computePersona(lateSubmitHour, totalDays, activeDays);
        List<String> badges = computeBadges(completedCount, levelDurationHours, lateSubmitHour, maxContinuousDays, rankPct);

        String reflectionText = loadReflectionText(mis);
        if (!StringUtils.hasText(reflectionText)) {
            reflectionText = DEFAULT_REFLECTION_TEXT;
        }

        boolean ready = completedCount >= 7
                && StringUtils.hasText(userInfo.getName())
                && totalDurationHours > 0
                && rankPct > 0
                && globalRank > 0
                && totalPassCount > 0
                && StringUtils.hasText((String) group.get("name"))
                && !levelDurationHours.isEmpty()
                && maxContinuousDays > 0
                && StringUtils.hasText(lateSubmitTime);

        Map<String, Object> response = new HashMap<>();
        response.put("ready", ready);
        response.put("userName", userInfo.getName());
        response.put("avatar", userInfo.getAvatar());
        response.put("completedCount", completedCount);
        response.put("totalDuration", totalDurationText);
        response.put("totalDurationHours", totalDurationHours);
        response.put("totalDays", totalDays);
        response.put("rankPct", rankPct);
        response.put("globalRank", globalRank);
        response.put("totalPassCount", totalPassCount);
        response.put("group", group);
        response.put("groupRankings", groupRankings);
        response.put("activeDays", activeDays);
        response.put("maxContinuousDays", maxContinuousDays);
        response.put("lateSubmitTime", lateSubmitTime);
        response.put("lateSubmitHour", lateSubmitHour);
        response.put("hardestLevel", hardestLevel != null ? hardestLevel : 0);
        response.put("levelDurationHours", levelDurationHours);
        response.put("levelTimeline", levelTimeline);
        response.put("persona", persona);
        response.put("badges", badges);
        response.put("reflectionText", reflectionText);
        response.put("defaultReflectionText", DEFAULT_REFLECTION_TEXT);

        return response;
    }

    /**
     * 获取排行榜缓存结果（TTL 5 分钟）。
     * 缓存命中直接返回，过期后重新计算并更新缓存。
     */
    private LeaderboardService.LeaderboardResponse getCachedGroupLeaderboard(String mis) {
        CachedLeaderboard cached = leaderboardCache.get(mis);
        if (cached != null && !cached.isExpired()) {
            log.debug("排行榜缓存命中，mis: {}", mis);
            return cached.data;
        }
        LeaderboardService.LeaderboardResponse fresh = leaderboardService.getGroupLeaderboard(mis, 50);
        leaderboardCache.put(mis, new CachedLeaderboard(fresh));
        return fresh;
    }

    /**
     * 保存闯关心得（最小改动实现：复用 user_stage_progress，stage_id=0 存储）
     */
    @PostMapping("/achievement-reflection")
    public Map<String, Object> saveAchievementReflection(@RequestBody Map<String, Object> body,
                                                         HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String reflectionText = body != null && body.get("reflectionText") != null
                ? String.valueOf(body.get("reflectionText")).trim() : "";

        if (reflectionText.length() > 60) {
            throw new IllegalArgumentException("心得字数不能超过60个字符");
        }

        UserStageProgress profile = userStageProgressDao.findByUserMisAndStageId(mis, REFLECTION_STAGE_ID);
        SsoUserInfo userInfo = ssoService.getUserInfo(mis);

        if (profile == null) {
            profile = new UserStageProgress();
            profile.setUserMis(mis);
            profile.setStageId(REFLECTION_STAGE_ID);
            profile.setVerifyStatus("pending");
            profile.setSubmittedValue(reflectionText);
            profile.setPassedAt(DEFAULT_PASSED_AT);
            profile.setLane("");
            profile.setUrl("");
            profile.setUserOrg(userInfo.getOrg() != null ? userInfo.getOrg() : "");
            profile.setUserOrgId(userInfo.getOrgId() != null ? userInfo.getOrgId() : "");
            userStageProgressDao.insert(profile);
        } else {
            profile.setVerifyStatus("pending");
            profile.setSubmittedValue(reflectionText);
            profile.setPassedAt(DEFAULT_PASSED_AT);
            profile.setLane("");
            profile.setUrl("");
            if (StringUtils.hasText(userInfo.getOrg())) {
                profile.setUserOrg(userInfo.getOrg());
            }
            if (StringUtils.hasText(userInfo.getOrgId())) {
                profile.setUserOrgId(userInfo.getOrgId());
            }
            userStageProgressDao.update(profile);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("ok", true);
        response.put("reflectionText", StringUtils.hasText(reflectionText) ? reflectionText : DEFAULT_REFLECTION_TEXT);
        return response;
    }

    private String loadReflectionText(String mis) {
        UserStageProgress profile = userStageProgressDao.findByUserMisAndStageId(mis, REFLECTION_STAGE_ID);
        if (profile == null) {
            return "";
        }
        return profile.getSubmittedValue() != null ? profile.getSubmittedValue().trim() : "";
    }

    private List<String> computeBadges(int completedCount,
                                       List<Long> levelDurationHours,
                                       int lateSubmitHour,
                                       int maxContinuousDays,
                                       int rankPct) {
        List<String> badges = new ArrayList<>();
        if (completedCount >= 7) {
            badges.add("fullstack_king");
        }

        // 闪电手：第5~7关单关用时 <=12h
        if (levelDurationHours != null && levelDurationHours.size() >= 7) {
            for (int i = 4; i < 7; i++) {
                Long hours = levelDurationHours.get(i);
                if (hours != null && hours > 0 && hours <= 12) {
                    badges.add("lightning_hand");
                    break;
                }
            }
        }

        if (lateSubmitHour >= 0 && lateSubmitHour < 6) {
            badges.add("night_owl");
        }

        if (maxContinuousDays >= 4) {
            badges.add("persistent");
        }

        if (rankPct >= 85) {
            badges.add("speed_runner");
        }

        return badges;
    }

    private String computePersona(int lateSubmitHour, int totalDays, int activeDays) {
        if (lateSubmitHour >= 0 && lateSubmitHour < 4) {
            return "deep_night_geek";
        }
        if (totalDays > 0 && (totalDays / 7.0) <= 1.5) {
            return "lightning_breaker";
        }
        if (activeDays >= 10) {
            return "steady_builder";
        }
        return "steady_runner";
    }

    private int countActiveDays(List<Date> stepTimes) {
        return toDateSet(stepTimes).size();
    }

    private int calcMaxContinuousDays(List<Date> stepTimes) {
        Set<LocalDate> dateSet = toDateSet(stepTimes);
        if (dateSet.isEmpty()) {
            return 0;
        }
        List<LocalDate> sortedDates = new ArrayList<>(dateSet);
        Collections.sort(sortedDates);

        int maxStreak = 1;
        int currentStreak = 1;
        for (int i = 1; i < sortedDates.size(); i++) {
            LocalDate prev = sortedDates.get(i - 1);
            LocalDate current = sortedDates.get(i);
            if (prev.plusDays(1).equals(current)) {
                currentStreak++;
                if (currentStreak > maxStreak) {
                    maxStreak = currentStreak;
                }
            } else {
                currentStreak = 1;
            }
        }
        return maxStreak;
    }

    /** [FIX-P1] 显式指定东八区，避免 JVM 系统时区影响活跃天数计算 */
    private Set<LocalDate> toDateSet(List<Date> stepTimes) {
        Set<LocalDate> dateSet = new HashSet<>();
        if (stepTimes == null) {
            return dateSet;
        }
        for (Date stepTime : stepTimes) {
            if (stepTime == null) {
                continue;
            }
            LocalDate localDate = Instant.ofEpochMilli(stepTime.getTime())
                    .atZone(ZONE_CST)
                    .toLocalDate();
            dateSet.add(localDate);
        }
        return dateSet;
    }

    private Date findLatestTime(List<Date> dates) {
        if (dates == null || dates.isEmpty()) {
            return null;
        }
        return Collections.max(dates, Comparator.comparingLong(Date::getTime));
    }

    private boolean isValidPassedAt(Date passedAt) {
        return passedAt != null && passedAt.after(DEFAULT_PASSED_AT);
    }

    private long calcDurationHours(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        if (diff <= 0) {
            return 0L;
        }
        return (long) Math.ceil(diff / 3600000.0D);
    }

    private String formatDuration(long totalHours) {
        if (totalHours <= 0) {
            return "0小时";
        }
        long days = totalHours / 24;
        long hours = totalHours % 24;
        if (days <= 0) {
            return hours + "小时";
        }
        if (hours <= 0) {
            return days + "天";
        }
        return days + "天" + hours + "小时";
    }

    /** [FIX-P1] 改用线程安全的 DateTimeFormatter，并显式使用东八区 */
    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }
        return DT_FMT.format(date.toInstant());
    }

    /** [FIX-P1] 改用线程安全的 DateTimeFormatter，并显式使用东八区 */
    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return TIME_FMT.format(date.toInstant());
    }

    /** [FIX-P1] 显式使用东八区提取小时数，避免服务器时区影响凌晨判断 */
    private int extractHour(Date date) {
        if (date == null) {
            return -1;
        }
        return LocalDateTime.ofInstant(date.toInstant(), ZONE_CST).getHour();
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private static Date parseDefaultPassedAt() {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1970-01-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
