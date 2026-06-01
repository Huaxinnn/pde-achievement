package com.meituan.pde.dao;

import com.meituan.pde.entity.BountySubmission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BountySubmissionDao {

    List<BountySubmission> findByTaskIdAndPhase(@Param("taskId") Long taskId, @Param("phase") String phase);

    BountySubmission findByTaskIdAndPhaseAndUser(@Param("taskId") Long taskId, @Param("phase") String phase, @Param("userMis") String userMis);

    BountySubmission findById(@Param("id") Long id);

    int insert(BountySubmission submission);

    int update(BountySubmission submission);

    int incrementVoteCount(@Param("id") Long id);

    int decrementVoteCount(@Param("id") Long id);

    int updateReview(@Param("id") Long id, @Param("status") String status, @Param("rejectReason") String rejectReason);

    int setWinner(@Param("id") Long id, @Param("taskId") Long taskId, @Param("phase") String phase);

    int countByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量查询某用户在多个 task 中提交过的 taskId+phase 列表
     * 用于列表接口展示"我的参与状态"
     */
    List<BountySubmission> findByUserMisAndTaskIds(@Param("userMis") String userMis, @Param("taskIds") List<Long> taskIds);
}
