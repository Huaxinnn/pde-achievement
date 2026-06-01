package com.meituan.pde.dao;

import com.meituan.pde.entity.DiscussionLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussionLikeDao {
    DiscussionLike findByPostIdAndUserMis(@Param("postId") Long postId, @Param("userMis") String userMis);

    List<DiscussionLike> findByPostIdsAndUserMis(@Param("postIds") List<Long> postIds, @Param("userMis") String userMis);

    int insert(DiscussionLike like);

    int deleteByPostIdAndUserMis(@Param("postId") Long postId, @Param("userMis") String userMis);
}
