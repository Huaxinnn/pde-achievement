package com.meituan.pde.controller;

import com.meituan.pde.dao.VerifyCheckinLogDao;
import com.meituan.pde.entity.VerifyCheckinLog;
import com.meituan.pde.util.AuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/verify")
public class VerifyController {

    @Autowired
    private VerifyCheckinLogDao verifyCheckinLogDao;

    @Value("${verify.token.secret:pde-secret-2024}")
    private String tokenSecret;

    /**
     * 获取当前用户的校验 token（前端加载关卡页时调用，token 拼入终端命令）
     */
    @GetMapping("/token")
    public Map<String, Object> getToken(HttpServletRequest request) {
        String mis = AuthUtils.getMisFromRequest(request);
        String token = generateToken(mis);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        return result;
    }

    /**
     * 终端命令执行后自动上报（curl 调用，记录版本信息）
     * 支持 GET 和 POST，方便 curl 命令直接调用
     */
    @GetMapping("/checkin")
    @ResponseBody
    public String checkin(@RequestParam(required = false) String mis,
                          @RequestParam(required = false) String token,
                          @RequestParam(required = false, defaultValue = "1") int stage,
                          @RequestParam(required = false, defaultValue = "") String v,
                          @RequestParam(required = false, defaultValue = "") String node,
                          @RequestParam(required = false, defaultValue = "") String git) {
        if (mis == null || token == null) {
            return "error: missing params";
        }
        // 验证 token
        String expectedToken = generateToken(mis);
        if (!expectedToken.equals(token)) {
            log.warn("checkin token 校验失败 mis={} stage={}", mis, stage);
            return "error: invalid token";
        }

        String versionInfo = buildVersionInfo(stage, v, node, git);
        VerifyCheckinLog record = new VerifyCheckinLog();
        record.setUserMis(mis);
        record.setStageId(stage);
        record.setVersionInfo(versionInfo);
        verifyCheckinLogDao.insert(record);
        log.info("checkin 上报成功 mis={} stage={} version={}", mis, stage, versionInfo);
        return "ok";
    }

    /** 生成 token：MD5(mis:today:secret)，每天轮换 */
    public String generateToken(String mis) {
        String raw = mis + ":" + LocalDate.now() + ":" + tokenSecret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private String buildVersionInfo(int stage, String v, String node, String git) {
        if (stage == 2) {
            return "node=" + node + " git=" + git;
        }
        return "v=" + v;
    }
}
