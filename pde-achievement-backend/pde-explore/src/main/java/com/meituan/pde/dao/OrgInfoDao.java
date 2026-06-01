package com.meituan.pde.dao;

import com.meituan.pde.entity.OrgInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrgInfoDao {
    int upsert(OrgInfo orgInfo);
    List<String> findAllOrgIds();
    List<Map<String, String>> findOrgIdNameMap();
    OrgInfo findByOrgId(@Param("orgId") String orgId);
    Long findEmpCountByOrgId(@Param("orgId") String orgId);
}
