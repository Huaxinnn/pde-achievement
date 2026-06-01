package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BountyMember {
    private Long id;
    private String userMis;
    private String userName;
    private String dept;
    private String role;
    private String customRole;
    private String reason;
    private Integer deletedFlag;
    private Date addTime;
    private Date createTime;
    private Date updateTime;
}
