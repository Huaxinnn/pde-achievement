package com.meituan.pde.common;

import lombok.Data;

@Data
public class SsoUserInfo {
    private String mis;
    private String name;
    private String avatar;
    private String org;
    private String orgId;
    private String fullOrgPath;
}
