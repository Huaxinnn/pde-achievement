package com.meituan.pde.dao;

import com.meituan.pde.entity.UserStageProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserStageProgressDao {
    UserStageProgress findByUserMisAndStageId(@Param("userMis") String userMis,
                                              @Param("stageId") Long stageId);
    List<UserStageProgress> findByUserMis(@Param("userMis") String userMis);
    Long findMaxPassedStageIdByUserMis(@Param("userMis") String userMis);
    List<UserStageProgress> findRecentPassed(@Param("limit") int limit);
    List<UserStageProgress> findAllPassed();
    String findLatestOrgIdByUserMis(@Param("userMis") String userMis);
    String findLatestOrgByUserMis(@Param("userMis") String userMis);
    int insert(UserStageProgress progress);
    int update(UserStageProgress progress);

    List<UserStageProgress> findPendingManual(@Param("status") String status,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);
    int countPendingManual(@Param("status") String status);
    UserStageProgress findById(@Param("id") Long id);
    int updateReview(UserStageProgress progress);

    List<String> findMisWithEmptyOrg(@Param("limit") int limit);
    List<String> findMisWithEmptyOrgId(@Param("limit") int limit);
    int updateOrgByMis(@Param("userMis") String userMis, @Param("userOrg") String userOrg);
    int updateOrgIdByMis(@Param("userMis") String userMis, @Param("userOrgId") String userOrgId);
    int countEmptyOrg();
}
