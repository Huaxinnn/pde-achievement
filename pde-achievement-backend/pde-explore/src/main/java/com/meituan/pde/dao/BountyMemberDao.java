package com.meituan.pde.dao;

import com.meituan.pde.entity.BountyMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BountyMemberDao {

    // 根据 mis 查找成员
    BountyMember findByUserMis(@Param("userMis") String userMis);

    // 插入新成员
    int insert(BountyMember member);

    // 更新成员信息（重新加入/修改角色等）
    int updateByUserMis(BountyMember member);

    // 统计总成员数
    int countAll();

    // 统计某用户前面有多少人（用于计算"你是第x个"）
    int countBefore(@Param("userMis") String userMis);

    // 查询所有共建成员，按加入时间升序
    List<BountyMember> findAll();
}
