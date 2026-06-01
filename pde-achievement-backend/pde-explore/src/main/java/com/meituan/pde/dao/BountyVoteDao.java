package com.meituan.pde.dao;

import com.meituan.pde.entity.BountyVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BountyVoteDao {

    // 查询某任务的所有投票（用于构建 votersMap）
    List<BountyVote> findByTaskId(@Param("taskId") Long taskId);

    // 查询用户对某任务的所有投票
    List<BountyVote> findByTaskIdAndUser(@Param("taskId") Long taskId, @Param("userMis") String userMis);

    // 查询用户是否已投某个具体方案
    BountyVote findByTaskIdAndSubmissionAndUser(@Param("taskId") Long taskId,
                                                @Param("submissionId") Long submissionId,
                                                @Param("userMis") String userMis);

    // 统计用户在某任务已投票数
    int countByTaskIdAndUser(@Param("taskId") Long taskId, @Param("userMis") String userMis);

    // 查询用户点赞过的所有 taskId（submission_id=0 代表对任务/创意本身的点赞）
    List<Long> findLikedTaskIdsByUser(@Param("userMis") String userMis);

    int insert(BountyVote vote);

    // 取消投票（物理删除，简单起见）
    int deleteByTaskIdAndSubmissionAndUser(@Param("taskId") Long taskId,
                                           @Param("submissionId") Long submissionId,
                                           @Param("userMis") String userMis);

    /**
     * 批量查询某用户对哪些任务投过票（submission_id > 0，排除点赞）
     * 返回 taskId 列表，用于列表接口展示"我的参与状态"
     */
    List<Long> findVotedTaskIdsByUser(@Param("userMis") String userMis, @Param("taskIds") List<Long> taskIds);
}
