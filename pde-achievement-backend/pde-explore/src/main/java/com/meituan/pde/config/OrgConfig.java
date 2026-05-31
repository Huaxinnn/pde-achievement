package com.meituan.pde.config;

import com.sankuai.meituan.org.opensdk.client.RemoteServiceClient;
import com.sankuai.meituan.org.opensdk.service.EmpService;
import com.sankuai.meituan.org.opensdk.service.OrgService;
import com.sankuai.meituan.org.opensdk.service.impl.EmpServiceImpl;
import com.sankuai.meituan.org.opensdk.service.impl.OrgServiceImpl;
import com.sankuai.meituan.org.queryservice.domain.param.DataScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.Arrays;

/**
 * Org OpenSDK 配置。
 * 提供 EmpService（查员工姓名）和 OrgService（查部门路径）两个 Bean。
 */
@Configuration
public class OrgConfig {

    // Org 网关固定 appkey，所有系统相同，无需修改
    private static final String REMOTE_APP_KEY = "com.sankuai.hrmdm.org.gateway";
    // secret 已废弃，传空字符串即可
    private static final String APP_SECRET = "";

    // Org 开放平台分配的应用编码，test/prod 不同，配置在各环境 application.properties
    @Value("${org.app-key}")
    private String appKey;

    // throws Exception：RemoteServiceClient 构造时若 Org 网关不可达，Spring 上下文启动失败。
    // 确保 Org 开放平台已完成授权（见 docs/sso-org-integration.md 待补充信息第 5 条）。
    @Bean
    @Lazy
    public RemoteServiceClient remoteServiceClient() throws Exception {
        DataScope dataScope = new DataScope();
        dataScope.setTenantId(1);
        dataScope.setSources(Arrays.asList("MT"));
        return new RemoteServiceClient(appKey, APP_SECRET, REMOTE_APP_KEY, dataScope);
    }

    @Bean
    @Lazy
    public EmpService empService(RemoteServiceClient remoteServiceClient) {
        return new EmpServiceImpl(remoteServiceClient);
    }

    @Bean
    @Lazy
    public OrgService orgService(RemoteServiceClient remoteServiceClient) {
        return new OrgServiceImpl(remoteServiceClient);
    }
}
