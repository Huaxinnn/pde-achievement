package com.meituan.pde.config;

import com.meituan.mdp.boot.starter.config.annotation.MdpConfig;
import org.springframework.stereotype.Component;

@Component
public class AdminConfig {

    /**
     * 管理员 mis 名单，逗号分隔，通过 Lion 配置动态更新。
     * 示例：zhang_san,li_si
     */
    @MdpConfig("pde.admin.mis.list:yanli06")
    public static String ADMIN_MIS_LIST = "yanli06";
}
