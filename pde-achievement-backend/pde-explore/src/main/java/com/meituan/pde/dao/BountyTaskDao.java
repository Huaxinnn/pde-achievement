package com.meituan.pde.dao;

import com.meituan.pde.entity.BountyTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BountyTaskDao {

    List<BountyTask> findAll(@Param("type") String type, @Param("status") String status);

    BountyTask findById(@Param("id") Long id);

    int insert(BountyTask task);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int updateFeatured(@Param("id") Long id, @Param("featuredBy") String featuredBy, @Param("featuredReason") String featuredReason);

    int updateTask(BountyTask task);

    int updateIdea(BountyTask task);

    int incrementLikeCount(@Param("id") Long id);

    int decrementLikeCount(@Param("id") Long id);

    int softDelete(@Param("id") Long id);

    List<BountyTask> findSimilarIdeas(@Param("keyword") String keyword, @Param("excludeId") Long excludeId);

    List<BountyTask> findSimilarIdeasByKeywords(@Param("keywords") List<String> keywords, @Param("excludeId") Long excludeId);
}
