package com.meituan.pde.service;

import com.meituan.pde.common.SsoUserInfo;
import com.sankuai.meituan.org.opensdk.model.domain.Emp;
import com.sankuai.meituan.org.opensdk.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SsoService {

    @Autowired
    @Lazy
    private EmpService empService;

    @Value("${pde.sso.remote.enabled:true}")
    private boolean ssoRemoteEnabled;

    /**
     * 查询单个用户信息。
     * 失败时 name 兜底为 mis，org 兜底为空字符串，不抛异常。
     */
    public SsoUserInfo getUserInfo(String mis) {
        SsoUserInfo info = buildFallback(mis);
        if (!StringUtils.hasText(mis)) {
            return info;
        }
        if (!ssoRemoteEnabled) {
            return info;
        }
        try {
            Emp emp = empService.queryByMis(mis, null);
            if (emp == null) {
                log.warn("[SsoService] Org 查询员工为空，mis={}", mis);
                return info;
            }

            info.setName(emp.getName() != null ? emp.getName() : mis);
            String orgName = emp.getOrgName() != null ? emp.getOrgName() : "";
            info.setOrg(orgName);
            info.setOrgId(emp.getOrgId() != null ? emp.getOrgId() : "");
            info.setFullOrgPath(orgName);
        } catch (Exception e) {
            log.error("[SsoService] Org 查询异常，mis={}，使用兜底值", mis, e);
        }
        return info;
    }

    /**
     * 批量查询用户信息（排行榜专用）。
     * 内部用 batchQueryByMis 一次拉取所有员工，org 直接取 emp.getOrgName()。
     * 查询失败的条目兜底填充，不抛异常。
     */
    public Map<String, SsoUserInfo> batchGetUserInfo(List<String> misList) {
        if (CollectionUtils.isEmpty(misList)) {
            return new HashMap<>();
        }
        Map<String, SsoUserInfo> result = misList.stream()
                .collect(Collectors.toMap(Function.identity(), this::buildFallback));
        if (!ssoRemoteEnabled) {
            return result;
        }
        try {
            List<Emp> emps = empService.batchQueryByMis(misList, null);
            if (CollectionUtils.isEmpty(emps)) {
                return result;
            }
            for (Emp emp : emps) {
                if (emp.getMis() == null) continue;
                SsoUserInfo info = result.get(emp.getMis());
                if (info == null) continue;
                info.setName(emp.getName() != null ? emp.getName() : emp.getMis());
                info.setOrg(emp.getOrgName() != null ? emp.getOrgName() : "");
                info.setOrgId(emp.getOrgId() != null ? emp.getOrgId() : "");
                info.setFullOrgPath(emp.getOrgName() != null ? emp.getOrgName() : "");
            }
        } catch (Exception e) {
            log.error("[SsoService] batchQueryByMis 异常，使用兜底值，misList={}", misList, e);
        }
        return result;
    }

    /**
     * 查询用户完整组织路径（不截断）。
     * 员工不存在返回空字符串；SDK 异常也返回空字符串（调用方可重试）。
     */
    public String getFullOrgPath(String mis) {
        if (!StringUtils.hasText(mis) || !ssoRemoteEnabled) return "";
        try {
            Emp emp = empService.queryByMis(mis, null);
            if (emp == null) return "";
            return emp.getOrgName() != null ? emp.getOrgName() : "";
        } catch (Exception e) {
            log.error("[SsoService] getFullOrgPath 查询异常，mis={}", mis, e);
            return "";
        }
    }

    /**
     * 查询用户完整组织路径，专供回填历史数据使用。
     * 员工不存在（已离职）返回 "已离职"；SDK 异常返回空字符串（不写入，下次重试）。
     */
    public String getFullOrgPathForBackfill(String mis) {
        if (!StringUtils.hasText(mis)) return "已离职";
        if (!ssoRemoteEnabled) return "";
        try {
            Emp emp = empService.queryByMis(mis, null);
            if (emp == null) return "已离职";
            String orgName = emp.getOrgName();
            return StringUtils.hasText(orgName) ? orgName : "";
        } catch (Exception e) {
            log.error("[SsoService] getFullOrgPathForBackfill 查询异常，mis={}", mis, e);
            return "";
        }
    }

    private SsoUserInfo buildFallback(String mis) {
        SsoUserInfo info = new SsoUserInfo();
        info.setMis(mis);
        info.setName(mis);
        info.setAvatar(null);
        info.setOrg("");
        info.setOrgId("");
        info.setFullOrgPath("");
        return info;
    }
}
