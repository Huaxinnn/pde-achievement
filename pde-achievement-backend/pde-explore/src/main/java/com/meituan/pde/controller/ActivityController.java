package com.meituan.pde.controller;

import com.meituan.pde.service.ActivityBroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/ulivepde/api/activity")
public class ActivityController {

    @Autowired
    private ActivityBroadcastService broadcastService;

    @GetMapping("/stream")
    public SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        log.info("新 SSE 连接建立");
        return broadcastService.subscribe();
    }

    // Oceanus 网关两次消息间隔超过15s会主动断连，心跳间隔设为10s
    @Scheduled(fixedDelay = 10000)
    public void heartbeat() {
        broadcastService.sendHeartbeat();
    }
}
