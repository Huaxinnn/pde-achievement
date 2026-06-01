package com.meituan.pde.controller;

import com.meituan.pde.service.LeaderboardService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/leaderboard")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    /**
     * 小组排行榜
     */
    @GetMapping("/group")
    public Map<String, Object> getGroupLeaderboard(HttpServletRequest request,
                                                   @RequestParam(defaultValue = "50") int limit) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取小组排行榜，mis: {}, limit: {}", mis, limit);
        try {
            LeaderboardService.LeaderboardResponse response = leaderboardService.getGroupLeaderboard(mis, limit);
            log.info("获取小组排行榜成功，mis: {}, 总条数: {}", mis, response.getList().size());
            return buildResponse(response, true);
        } catch (Exception e) {
            log.error("获取小组排行榜失败，mis: {}", mis, e);
            throw e;
        }
    }

    /**
     * 全公司排行榜
     */
    @GetMapping("/meituan")
    public Map<String, Object> getMeituanLeaderboard(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "50") int limit) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取全公司排行榜，mis: {}, limit: {}", mis, limit);
        try {
            LeaderboardService.LeaderboardResponse response = leaderboardService.getMeituanLeaderboard(mis, limit);
            log.info("获取全公司排行榜成功，mis: {}, 总条数: {}", mis, response.getList().size());
            return buildResponse(response, false);
        } catch (Exception e) {
            log.error("获取全公司排行榜失败，mis: {}", mis, e);
            throw e;
        }
    }

    private Map<String, Object> buildResponse(LeaderboardService.LeaderboardResponse response, boolean includeGroupMeta) {
        List<Map<String, Object>> list = response.getList().stream().map(entry -> {
            Map<String, Object> item = new HashMap<>();
            item.put("rank", entry.getRank());
            item.put("mis", entry.getMis());
            item.put("name", entry.getName());
            item.put("avatar", entry.getAvatar());
            item.put("org", entry.getOrg());
            item.put("stageName", entry.getStageName());
            item.put("completedCount", entry.getCompletedCount());
            item.put("lastPassedAt", entry.getLastPassedAt());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("myRank", response.getMyRank());
        result.put("globalRank", response.getGlobalRank());
        result.put("totalPassCount", response.getTotalPassCount());
        result.put("rankPct", response.getRankPct());
        if (includeGroupMeta && response.getGroup() != null) {
            Map<String, Object> group = new HashMap<>();
            group.put("name", response.getGroup().getName());
            group.put("myRank", response.getGroup().getMyRank());
            group.put("totalMembers", response.getGroup().getTotalMembers());
            group.put("passCount", response.getGroup().getPassCount());
            result.put("group", group);
        }
        result.put("list", list);
        return result;
    }
}
