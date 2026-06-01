package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class OrgInfo {
    private Long id;
    private String orgId;
    private String orgName;
    private Long empCount;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
