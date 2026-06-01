package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class UserStepProgress {
    private Long id;
    private String userMis;
    private String userOrg;
    private String userOrgId;
    private Long stepId;
    private Date completedAt;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;

    /**
     * 非 DB 列，由 findAllByUserMis 联查 stage_step.stage_id 填入，
     * 用于成就报告批量加载后按关卡分组，避免 N+1 查询。
     */
    private Long stageId;
}
