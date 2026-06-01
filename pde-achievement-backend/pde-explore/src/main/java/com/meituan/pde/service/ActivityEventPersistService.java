package com.meituan.pde.service;

import com.meituan.pde.common.ActivityEvent;
import com.meituan.pde.dao.ActivityEventDao;
import com.meituan.pde.entity.ActivityEventRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class ActivityEventPersistService {

    @Autowired
    private ActivityEventDao activityEventDao;

    @Async
    public void persistAsync(ActivityEvent event) {
        try {
            ActivityEventRecord record = new ActivityEventRecord();
            record.setUserMis(event.getMis());
            record.setUserName(event.getName());
            record.setEventType(event.getType());
            record.setStageId((long) event.getStageId());
            record.setStageName(event.getStageTitle());
            record.setOccurredAt(new Date(event.getTimestamp()));
            activityEventDao.insert(record);
        } catch (Exception e) {
            log.error("activity_event 异步写库失败，event={}", event.getType(), e);
        }
    }
}
