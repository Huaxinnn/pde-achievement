package com.meituan.pde.dao;

import com.meituan.pde.entity.VerifyCheckinLog;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface VerifyCheckinLogDao {

    /** 插入一条上报记录 */
    void insert(VerifyCheckinLog log);

    /** 查询用户在某关卡 minTime 之后是否有上报记录，返回记录数（兼容 SQLite 和 MySQL） */
    int countCheckinAfter(@Param("userMis") String userMis,
                          @Param("stageId") int stageId,
                          @Param("minTime") Date minTime);
}
