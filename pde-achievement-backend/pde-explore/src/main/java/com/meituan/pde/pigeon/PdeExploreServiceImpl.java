package com.meituan.pde.pigeon;

import com.meituan.mdp.boot.starter.pigeon.annotation.MdpPigeonServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * PDE 闯关系统 Pigeon 服务实现（满足公司统一接入要求）
 */
@MdpPigeonServer
@ConditionalOnProperty(name = "pde.pigeon.enabled", havingValue = "true", matchIfMissing = true)
public class PdeExploreServiceImpl implements PdeExploreService {

    @Override
    public String ping() {
        return "pong";
    }
}
