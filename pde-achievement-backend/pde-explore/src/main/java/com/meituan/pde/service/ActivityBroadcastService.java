package com.meituan.pde.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.pde.common.ActivityEvent;
import com.meituan.pde.dao.ActivityEventDao;
import com.meituan.pde.entity.ActivityEventRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class ActivityBroadcastService {

    private static final int HISTORY_SIZE = 50;
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ActivityEventDao activityEventDao;

    @Autowired
    private ActivityEventPersistService persistService;

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        try {
            List<ActivityEventRecord> records = activityEventDao.findRecent(HISTORY_SIZE);
            // findRecent 返回 DESC（最新在前），倒序推送使前端展示顺序正确
            for (int i = records.size() - 1; i >= 0; i--) {
                sendToEmitter(emitter, toEvent(records.get(i)));
            }
        } catch (Exception e) {
            log.warn("subscribe 回放历史失败", e);
        }
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(ActivityEvent event) {
        // 异步持久化到数据库，不阻塞 SSE 推送线程
        if (event.getStageId() > 0) {
            persistService.persistAsync(event);
        }

        // 广播给所有在线连接
        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, event);
        }
    }

    public void sendHeartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    // 每天凌晨 3 点清理 90 天前的记录
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanOldEvents() {
        try {
            int deleted = activityEventDao.deleteOlderThan(90);
            log.info("activity_event 清理完成，删除 {} 条 90 天前的记录", deleted);
        } catch (Exception e) {
            log.error("activity_event 清理失败", e);
        }
    }

    private ActivityEvent toEvent(ActivityEventRecord r) {
        long ts = r.getOccurredAt() != null ? r.getOccurredAt().getTime() : System.currentTimeMillis();
        return new ActivityEvent(r.getId(), r.getUserMis(), r.getUserName(), r.getEventType(), r.getStageId().intValue(), r.getStageName(), ts);
    }

    private void sendToEmitter(SseEmitter emitter, ActivityEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().data(json));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
    }
}
