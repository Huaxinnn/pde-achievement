package com.meituan.pde.dao;

import com.meituan.pde.entity.StageStep;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StageStepDao {
    List<StageStep> findByStageId(@Param("stageId") Long stageId);
    StageStep findById(@Param("id") Long id);
    int insert(StageStep stageStep);
    int update(StageStep stageStep);
}
