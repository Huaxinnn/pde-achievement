package com.meituan.pde.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DefinePrototype {
    private Long id;
    private String docId;
    private String title;
    private String htmlContent;
    private String authorMis;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
