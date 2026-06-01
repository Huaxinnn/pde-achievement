package com.meituan.pde.dao;

import com.meituan.pde.entity.ActivityEventRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityEventDao {
    int insert(ActivityEventRecord record);
    List<ActivityEventRecord> findRecent(@Param("limit") int limit);
    int deleteOlderThan(@Param("days") int days);
}
