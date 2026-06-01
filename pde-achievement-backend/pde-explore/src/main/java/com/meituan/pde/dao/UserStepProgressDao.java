package com.meituan.pde.dao;

import com.meituan.pde.entity.UserStepProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserStepProgressDao {
    List<UserStepProgress> findByUserMisAndStageId(@Param("userMis") String userMis,
                                                   @Param("stageId") Long stageId);
    /** 批量查询用户所有步骤记录（用于成就报告，避免 N+1 查询） */
    List<UserStepProgress> findAllByUserMis(@Param("userMis") String userMis);
    int countByUserMisAndStageId(@Param("userMis") String userMis,
                                 @Param("stageId") Long stageId);
    int existsByUserMisAndStepId(@Param("userMis") String userMis,
                                 @Param("stepId") Long stepId);
    int insertIgnore(UserStepProgress progress);
    // 查有步骤记录但关卡未通关的用户+关卡，用于冷启动"正在冲刺"历史
    List<Map<String, Object>> findInProgressUserStages(@Param("limit") int limit);

    List<String> findMisWithEmptyOrg(@Param("limit") int limit);
    List<String> findMisWithEmptyOrgId(@Param("limit") int limit);
    int updateOrgByMis(@Param("userMis") String userMis, @Param("userOrg") String userOrg);
    int updateOrgIdByMis(@Param("userMis") String userMis, @Param("userOrgId") String userOrgId);
    int countEmptyOrg();
}
