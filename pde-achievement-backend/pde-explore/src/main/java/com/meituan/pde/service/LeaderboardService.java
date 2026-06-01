package com.meituan.pde.service;

import com.meituan.pde.common.SsoUserInfo;
import com.meituan.pde.dao.OrgInfoDao;
import com.meituan.pde.dao.StageDao;
import com.meituan.pde.dao.UserStageProgressDao;
import com.meituan.pde.entity.Stage;
import com.meituan.pde.entity.UserStageProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private UserStageProgressDao userStageProgressDao;

    @Autowired
    private StageDao stageDao;

    @Autowired
    private SsoService ssoService;

    @Autowired
    private OrgInfoDao orgInfoDao;

    /**
     * 获取小组排行榜
     */
    public LeaderboardResponse getGroupLeaderboard(String userMis, int limit) {
        List<Stage> allStages = stageDao.findAll();
        Map<Long, String> stageNameMap = allStages.stream()
                .collect(Collectors.toMap(Stage::getId, Stage::getName));

        List<UserStageProgress> allPassed = userStageProgressDao.findAllPassed();
        if (allPassed.isEmpty()) {
            LeaderboardResponse empty = new LeaderboardResponse();
            empty.setMyRank(0);
            empty.setGlobalRank(0);
            empty.setTotalPassCount(0);
            empty.setRankPct(0);
            empty.setList(new ArrayList<>());
            GroupMeta groupMeta = new GroupMeta();
            groupMeta.setName("");
            groupMeta.setMyRank(0);
            groupMeta.setTotalMembers(0);
            groupMeta.setPassCount(0);
            empty.setGroup(groupMeta);
            return empty;
        }

        Map<String, List<UserStageProgress>> passedByUser = allPassed.stream()
                .collect(Collectors.groupingBy(UserStageProgress::getUserMis));

        Map<String, UserStats> allStatsMap = new HashMap<>();
        for (Map.Entry<String, List<UserStageProgress>> entry : passedByUser.entrySet()) {
            allStatsMap.put(entry.getKey(), buildStats(entry.getKey(), entry.getValue(), stageNameMap));
        }

        List<LeaderboardEntry> globalEntries = buildEntries(allStatsMap);
        sortAndRankEntries(globalEntries);
        int globalRank = findMyRank(globalEntries, userMis);
        int totalPassCount = globalEntries.size();
        int rankPct = calcRankPct(globalRank, totalPassCount);

        SsoUserInfo meInfo = ssoService.getUserInfo(userMis);
        String myOrgId = meInfo.getOrgId();
        if ((myOrgId == null || myOrgId.isEmpty())) {
            myOrgId = userStageProgressDao.findLatestOrgIdByUserMis(userMis);
        }

        Map<String, UserStats> groupStatsMap = new HashMap<>();
        for (Map.Entry<String, UserStats> entry : allStatsMap.entrySet()) {
            UserStats stats = entry.getValue();
            if (myOrgId != null && !myOrgId.isEmpty() && !myOrgId.equals(stats.getUserOrgId())) {
                continue;
            }
            groupStatsMap.put(entry.getKey(), stats);
        }

        List<LeaderboardEntry> entries = buildEntries(groupStatsMap);
        sortAndRankEntries(entries);
        int groupRank = findMyRank(entries, userMis);

        int totalMembers = 0;
        if (myOrgId != null && !myOrgId.isEmpty()) {
            Long empCount = orgInfoDao.findEmpCountByOrgId(myOrgId);
            totalMembers = empCount != null ? empCount.intValue() : 0;
        }
        if (totalMembers <= 0) {
            totalMembers = entries.size();
        }

        String groupName = meInfo.getOrg();
        if ((groupName == null || groupName.isEmpty())) {
            groupName = userStageProgressDao.findLatestOrgByUserMis(userMis);
        }

        LeaderboardResponse response = new LeaderboardResponse();
        response.setMyRank(groupRank);
        response.setGlobalRank(globalRank);
        response.setTotalPassCount(totalPassCount);
        response.setRankPct(rankPct);
        response.setList(entries.stream().limit(limit).collect(Collectors.toList()));

        GroupMeta groupMeta = new GroupMeta();
        groupMeta.setName(groupName != null ? groupName : "");
        groupMeta.setMyRank(groupRank);
        groupMeta.setTotalMembers(totalMembers);
        groupMeta.setPassCount(entries.size());
        response.setGroup(groupMeta);
        return response;
    }

    /**
     * 获取全公司排行榜
     */
    public LeaderboardResponse getMeituanLeaderboard(String userMis, int limit) {
        List<Stage> allStages = stageDao.findAll();
        Map<Long, String> stageNameMap = allStages.stream()
                .collect(Collectors.toMap(Stage::getId, Stage::getName));

        List<UserStageProgress> allPassed = userStageProgressDao.findAllPassed();
        if (allPassed.isEmpty()) {
            LeaderboardResponse empty = new LeaderboardResponse();
            empty.setMyRank(0);
            empty.setGlobalRank(0);
            empty.setTotalPassCount(0);
            empty.setRankPct(0);
            empty.setList(new ArrayList<>());
            return empty;
        }

        Map<String, List<UserStageProgress>> passedByUser = allPassed.stream()
                .collect(Collectors.groupingBy(UserStageProgress::getUserMis));
        Map<String, UserStats> statsMap = new HashMap<>();
        for (Map.Entry<String, List<UserStageProgress>> entry : passedByUser.entrySet()) {
            statsMap.put(entry.getKey(), buildStats(entry.getKey(), entry.getValue(), stageNameMap));
        }

        List<LeaderboardEntry> entries = buildEntries(statsMap);
        sortAndRankEntries(entries);

        int myRank = findMyRank(entries, userMis);
        int totalPassCount = entries.size();
        int rankPct = calcRankPct(myRank, totalPassCount);

        LeaderboardResponse response = new LeaderboardResponse();
        response.setMyRank(myRank);
        response.setGlobalRank(myRank);
        response.setTotalPassCount(totalPassCount);
        response.setRankPct(rankPct);
        response.setList(entries.stream().limit(limit).collect(Collectors.toList()));
        return response;
    }

    private UserStats buildStats(String userMis, List<UserStageProgress> passed,
                                 Map<Long, String> stageNameMap) {
        UserStats stats = new UserStats();
        stats.setUserMis(userMis);
        stats.setCompletedCount(passed.size());

        UserStageProgress maxStage = passed.stream()
                .max(Comparator.comparing(UserStageProgress::getStageId))
                .orElse(null);
        if (maxStage != null) {
            stats.setMaxStageName(stageNameMap.getOrDefault(maxStage.getStageId(), "未知"));
            stats.setLastPassedAt(maxStage.getPassedAt());
        }

        passed.stream()
                .map(UserStageProgress::getUserOrg)
                .filter(org -> org != null && !org.isEmpty())
                .findFirst()
                .ifPresent(stats::setUserOrg);

        String orgId = findOrgIdFromPassed(passed);
        stats.setUserOrgId(orgId != null ? orgId : "");
        return stats;
    }

    private String findOrgIdFromPassed(List<UserStageProgress> passed) {
        return passed.stream()
                .map(UserStageProgress::getUserOrgId)
                .filter(orgId -> orgId != null && !orgId.isEmpty())
                .findFirst()
                .orElse("");
    }

    private void sortAndRankEntries(List<LeaderboardEntry> entries) {
        entries.sort((a, b) -> {
            int cmp = b.getCompletedCount().compareTo(a.getCompletedCount());
            if (cmp != 0) return cmp;
            Date aDate = a.getLastPassedAt();
            Date bDate = b.getLastPassedAt();
            if (aDate == null && bDate == null) return 0;
            if (aDate == null) return 1;
            if (bDate == null) return -1;
            return aDate.compareTo(bDate);
        });
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
    }

    private int findMyRank(List<LeaderboardEntry> entries, String userMis) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getMis().equals(userMis)) {
                return i + 1;
            }
        }
        return 0;
    }

    private int calcRankPct(int myRank, int totalPassCount) {
        if (myRank <= 0 || totalPassCount <= 0) {
            return 0;
        }
        return (int) Math.floor((totalPassCount - myRank) * 100.0 / totalPassCount);
    }

    private List<LeaderboardEntry> buildEntries(Map<String, UserStats> statsMap) {
        if (statsMap.isEmpty()) {
            return new ArrayList<>();
        }
        // 批量查询所有用户信息，替代原来的 N 次逐个调用
        List<String> misList = new ArrayList<>(statsMap.keySet());
        Map<String, SsoUserInfo> userInfoMap = ssoService.batchGetUserInfo(misList);

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (UserStats stats : statsMap.values()) {
            SsoUserInfo userInfo = userInfoMap.getOrDefault(stats.getUserMis(), buildFallbackUserInfo(stats.getUserMis()));
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setMis(stats.getUserMis());
            entry.setName(userInfo.getName());
            entry.setAvatar(userInfo.getAvatar());
            String orgFromDb = stats.getUserOrg();
            String org = orgFromDb != null && !orgFromDb.isEmpty() ? orgFromDb : userInfo.getOrg();
            entry.setOrg(org != null ? org : "");
            entry.setCompletedCount(stats.getCompletedCount());
            entry.setStageName(stats.getMaxStageName());
            entry.setLastPassedAt(stats.getLastPassedAt());
            entries.add(entry);
        }
        return entries;
    }

    private SsoUserInfo buildFallbackUserInfo(String mis) {
        SsoUserInfo info = new SsoUserInfo();
        info.setMis(mis);
        info.setName(mis);
        info.setAvatar(null);
        info.setOrg("");
        info.setOrgId("");
        info.setFullOrgPath("");
        return info;
    }

    private static class UserStats {
        private String userMis;
        private String userOrg;
        private String userOrgId;
        private int completedCount;
        private String maxStageName;
        private Date lastPassedAt;

        public String getUserMis() { return userMis; }
        public void setUserMis(String userMis) { this.userMis = userMis; }
        public String getUserOrg() { return userOrg; }
        public void setUserOrg(String userOrg) { this.userOrg = userOrg; }
        public String getUserOrgId() { return userOrgId; }
        public void setUserOrgId(String userOrgId) { this.userOrgId = userOrgId; }
        public int getCompletedCount() { return completedCount; }
        public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
        public String getMaxStageName() { return maxStageName; }
        public void setMaxStageName(String maxStageName) { this.maxStageName = maxStageName; }
        public Date getLastPassedAt() { return lastPassedAt; }
        public void setLastPassedAt(Date lastPassedAt) { this.lastPassedAt = lastPassedAt; }
    }

    public static class LeaderboardEntry {
        private Integer rank;
        private String mis;
        private String name;
        private String avatar;
        private String org;
        private String stageName;
        private Integer completedCount;
        private Date lastPassedAt;

        public Integer getRank() { return rank; }
        public void setRank(Integer rank) { this.rank = rank; }
        public String getMis() { return mis; }
        public void setMis(String mis) { this.mis = mis; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getOrg() { return org; }
        public void setOrg(String org) { this.org = org; }
        public String getStageName() { return stageName; }
        public void setStageName(String stageName) { this.stageName = stageName; }
        public Integer getCompletedCount() { return completedCount; }
        public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
        public Date getLastPassedAt() { return lastPassedAt; }
        public void setLastPassedAt(Date lastPassedAt) { this.lastPassedAt = lastPassedAt; }
    }

    public static class GroupMeta {
        private String name;
        private Integer myRank;
        private Integer totalMembers;
        private Integer passCount;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getMyRank() { return myRank; }
        public void setMyRank(Integer myRank) { this.myRank = myRank; }
        public Integer getTotalMembers() { return totalMembers; }
        public void setTotalMembers(Integer totalMembers) { this.totalMembers = totalMembers; }
        public Integer getPassCount() { return passCount; }
        public void setPassCount(Integer passCount) { this.passCount = passCount; }
    }

    public static class LeaderboardResponse {
        private Integer myRank;
        private Integer globalRank;
        private Integer totalPassCount;
        private Integer rankPct;
        private GroupMeta group;
        private List<LeaderboardEntry> list;

        public Integer getMyRank() { return myRank; }
        public void setMyRank(Integer myRank) { this.myRank = myRank; }
        public Integer getGlobalRank() { return globalRank; }
        public void setGlobalRank(Integer globalRank) { this.globalRank = globalRank; }
        public Integer getTotalPassCount() { return totalPassCount; }
        public void setTotalPassCount(Integer totalPassCount) { this.totalPassCount = totalPassCount; }
        public Integer getRankPct() { return rankPct; }
        public void setRankPct(Integer rankPct) { this.rankPct = rankPct; }
        public GroupMeta getGroup() { return group; }
        public void setGroup(GroupMeta group) { this.group = group; }
        public List<LeaderboardEntry> getList() { return list; }
        public void setList(List<LeaderboardEntry> list) { this.list = list; }
    }
}
