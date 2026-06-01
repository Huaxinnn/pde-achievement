package com.meituan.pde.dao;

import com.meituan.pde.entity.StageFaq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StageFaqDao {
    List<StageFaq> findByStageId(@Param("stageId") Long stageId);
    StageFaq findById(@Param("id") Long id);
    int insert(StageFaq stageFaq);
    int update(StageFaq stageFaq);
}
