package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DefineComment {
    private Long id;
    private String docId;
    private String section;
    private Long parentId;
    private String userMis;
    private String content;
    private String status;
    private Date createTime;
    private Date updateTime;
}
