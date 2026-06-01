package com.meituan.pde.entity;

import lombok.Data;
import java.util.Date;

@Data
public class VerifyCheckinLog {
    private Long id;
    private String userMis;
    private Integer stageId;
    private String versionInfo;
    private Date createdAt;
}
