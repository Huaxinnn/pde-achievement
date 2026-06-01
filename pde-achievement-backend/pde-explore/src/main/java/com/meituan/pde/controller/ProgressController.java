package com.meituan.pde.controller;

import com.meituan.pde.service.ProgressService;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/progress")
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    /**
     * 标记步骤完成（幂等）
     */
    @PostMapping("/step")
    public Map<String, Object> markStepComplete(@RequestBody Map<String, Object> body,
                                                HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        Long stageId = body.get("stageId") != null ? ((Number) body.get("stageId")).longValue() : null;
        Long stepId = body.get("stepId") != null ? ((Number) body.get("stepId")).longValue() : null;
        log.info("标记步骤完成，mis: {}, stageId: {}, stepId: {}", mis, stageId, stepId);

        if (stageId == null || stepId == null) {
            log.warn("标记步骤完成参数缺失，mis: {}, stageId: {}, stepId: {}", mis, stageId, stepId);
            throw new IllegalArgumentException("Missing required parameters: stageId, stepId");
        }

        try {
            ProgressService.StepCompleteResponse resp = progressService.markStepComplete(mis, stageId, stepId);
            log.info("标记步骤完成成功，mis: {}, stageId: {}, stepId: {}, stageCompleted: {}", mis, stageId, stepId, resp.getStageCompleted());
            Map<String, Object> result = new HashMap<>();
            result.put("ok", resp.getOk());
            result.put("stageCompleted", resp.getStageCompleted());
            return result;
        } catch (Exception e) {
            log.error("标记步骤完成失败，mis: {}, stageId: {}, stepId: {}", mis, stageId, stepId, e);
            throw e;
        }
    }

    /**
     * 提交通关验证
     */
    @PostMapping("/verify")
    public Map<String, Object> submitVerify(@RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        Long stageId = body.get("stageId") != null ? ((Number) body.get("stageId")).longValue() : null;
        String value = (String) body.get("value");
        String lane = (String) body.get("lane");
        String url = (String) body.get("url");
        log.info("提交通关验证，mis: {}, stageId: {}, value: {}", mis, stageId, value);

        if (stageId == null) {
            log.warn("提交通关验证参数缺失，mis: {}, stageId: {}", mis, stageId);
            throw new IllegalArgumentException("Missing required parameter: stageId");
        }
        // value 对 curl 类型不是必填，其他类型由 service 层处理

        try {
            ProgressService.VerifyResponse resp = progressService.submitVerify(mis, stageId, value, lane, url);
            log.info("提交通关验证成功，mis: {}, stageId: {}, verifyStatus: {}", mis, stageId, resp.getVerifyStatus());
            Map<String, Object> result = new HashMap<>();
            result.put("verifyStatus", resp.getVerifyStatus());
            result.put("message", resp.getMessage());
            result.put("stageCompleted", resp.getStageCompleted());
            return result;
        } catch (Exception e) {
            log.error("提交通关验证失败，mis: {}, stageId: {}", mis, stageId, e);
            throw e;
        }
    }
}
