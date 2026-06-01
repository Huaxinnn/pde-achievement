package com.meituan.pde.controller;

import com.meituan.pde.dao.StageDao;
import com.meituan.pde.dao.StageFaqDao;
import com.meituan.pde.dao.StageStepDao;
import com.meituan.pde.dao.UserStepProgressDao;
import com.meituan.pde.entity.Stage;
import com.meituan.pde.entity.StageFaq;
import com.meituan.pde.entity.StageStep;
import com.meituan.pde.entity.UserStageProgress;
import com.meituan.pde.entity.UserStepProgress;
import com.meituan.pde.service.ProgressService;
import com.meituan.pde.util.AuthUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/stages")
public class StageController {

    @Autowired
    private StageDao stageDao;

    @Autowired
    private StageStepDao stageStepDao;

    @Autowired
    private StageFaqDao stageFaqDao;

    @Autowired
    private UserStepProgressDao userStepProgressDao;

    @Autowired
    private ProgressService progressService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有关卡列表（含用户完成状态）
     */
    @GetMapping
    public Map<String, Object> getAllStages(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取关卡列表，mis: {}", mis);
        try {
            List<Stage> stages = stageDao.findAll();
            Long maxPassedStageId = progressService.getMaxPassedStageId(mis);
            int completedCount = progressService.getPassedStageCount(mis);

            Long currentStageId = maxPassedStageId != null ? maxPassedStageId + 1 : 1L;

            Map<String, Object> response = new HashMap<>();
            response.put("currentStage", currentStageId);
            response.put("completedCount", completedCount);
            response.put("totalCount", stages.size());

            List<Map<String, Object>> stageList = stages.stream().map(stage -> {
                Map<String, Object> stageMap = new HashMap<>();
                stageMap.put("id", stage.getId());
                stageMap.put("name", stage.getName());
                stageMap.put("title", stage.getTitle());
                stageMap.put("description", stage.getDescription());
                stageMap.put("completed", stage.getId() <= (maxPassedStageId != null ? maxPassedStageId : 0L));
                return stageMap;
            }).collect(Collectors.toList());

            response.put("stages", stageList);
            log.info("获取关卡列表成功，mis: {}, totalCount: {}", mis, stages.size());
            return response;
        } catch (Exception e) {
            log.error("获取关卡列表失败，mis: {}", mis, e);
            throw e;
        }
    }

    /**
     * 获取单个关卡详情（步骤 + FAQ + 验证信息）
     */
    @GetMapping("/{id}")
    public Map<String, Object> getStageDetail(@PathVariable Long id, HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        log.info("获取关卡详情，mis: {}, stageId: {}", mis, id);

        try {
            Stage stage = stageDao.findById(id);
            if (stage == null) {
                log.warn("关卡不存在，stageId: {}", id);
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Stage not found: " + id);
                return err;
            }

            List<StageStep> steps = stageStepDao.findByStageId(id);
            List<StageFaq> faqs = stageFaqDao.findByStageId(id);

            // 获取用户在该关卡已完成的步骤 ID 集合
            List<UserStepProgress> completedSteps = userStepProgressDao.findByUserMisAndStageId(mis, id);
            Set<Long> completedStepIds = completedSteps.stream()
                    .map(UserStepProgress::getStepId)
                    .collect(Collectors.toSet());

            // 获取用户在该关卡的通关记录
            UserStageProgress stageProgress = progressService.getStageProgress(mis, id);
            String verifyStatus = stageProgress != null ? stageProgress.getVerifyStatus() : "none";
            String submittedValue = stageProgress != null && stageProgress.getSubmittedValue() != null
                    ? stageProgress.getSubmittedValue() : "";
            String lane = stageProgress != null && stageProgress.getLane() != null
                    ? stageProgress.getLane() : "";
            String url = stageProgress != null && stageProgress.getUrl() != null
                    ? stageProgress.getUrl() : "";
            String rejectReason = stageProgress != null && stageProgress.getRejectReason() != null
                    ? stageProgress.getRejectReason() : "";

            Map<String, Object> response = new HashMap<>();
            response.put("id", stage.getId());
            response.put("name", stage.getName());
            response.put("title", stage.getTitle());
            response.put("description", stage.getDescription());
            response.put("verifyType", stage.getVerifyType());
            response.put("verifyHint", stage.getVerifyHint());
            response.put("verifyStatus", verifyStatus);
            response.put("submittedValue", submittedValue);
            response.put("lane", lane);
            response.put("url", url);
            response.put("rejectReason", rejectReason);

            List<Map<String, Object>> stepList = steps.stream().map(step -> {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("id", step.getId());
                stepMap.put("title", step.getTitle());
                stepMap.put("description", step.getDescription());
                try {
                    List<String> commandsList = objectMapper.readValue(
                            step.getCommands(), new TypeReference<List<String>>() {});
                    stepMap.put("commands", commandsList);
                } catch (Exception e) {
                    stepMap.put("commands", new ArrayList<>());
                }
                stepMap.put("tips", step.getTips());
                stepMap.put("completed", completedStepIds.contains(step.getId()));
                return stepMap;
            }).collect(Collectors.toList());

            response.put("steps", stepList);

            List<Map<String, Object>> faqList = faqs.stream().map(faq -> {
                Map<String, Object> faqMap = new HashMap<>();
                faqMap.put("question", faq.getQuestion());
                faqMap.put("answer", faq.getAnswer());
                return faqMap;
            }).collect(Collectors.toList());

            response.put("faqs", faqList);
            log.info("获取关卡详情成功，mis: {}, stageId: {}", mis, id);
            return response;
        } catch (Exception e) {
            log.error("获取关卡详情失败，mis: {}, stageId: {}", mis, id, e);
            throw e;
        }
    }
}
