package com.meituan.pde.dao;

import com.meituan.pde.entity.Stage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StageDao {
    List<Stage> findAll(); // 只返回 is_active=1 的关卡
    Stage findById(@Param("id") Long id);
    int insert(Stage stage);
    int update(Stage stage);
}
