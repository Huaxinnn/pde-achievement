package com.meituan.pde.dao;

import com.meituan.pde.entity.DiscussionPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussionPostDao {
    List<DiscussionPost> findTopByStageId(@Param("stageId") Long stageId);

    List<DiscussionPost> findRepliesByParentIds(@Param("parentIds") List<Long> parentIds);

    int insert(DiscussionPost post);

    DiscussionPost findById(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);

    int decrementLikeCount(@Param("id") Long id);

    int deleteById(@Param("id") Long id, @Param("userMis") String userMis);
}
