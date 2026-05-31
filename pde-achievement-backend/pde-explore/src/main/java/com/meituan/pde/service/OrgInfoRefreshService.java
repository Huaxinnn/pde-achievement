package com.meituan.pde.service;

import com.meituan.pde.dao.OrgInfoDao;
import com.meituan.pde.entity.OrgInfo;
import com.sankuai.meituan.org.opensdk.model.domain.items.EmpItems;
import com.sankuai.meituan.org.queryservice.domain.base.Paging;
import com.sankuai.meituan.org.treeservice.domain.EmpHierarchyCond;
import com.sankuai.meituan.org.opensdk.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OrgInfoRefreshService {

    @Autowired
    private OrgInfoDao orgInfoDao;

    @Autowired
    @Lazy
    private EmpService empService;

    /**
     * 每周一凌晨3点刷新一次。
     * 查 DB 里所有出现过的 user_org_id，调 queryByOrgIds 拿在职人数，upsert 进 org_info。
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void refresh() {
        log.info("[OrgInfoRefresh] 开始刷新组织信息");
        try {
            List<Map<String, String>> orgList = orgInfoDao.findOrgIdNameMap();
            if (CollectionUtils.isEmpty(orgList)) {
                log.info("[OrgInfoRefresh] 没有需要刷新的组织ID");
                return;
            }
            log.info("[OrgInfoRefresh] 共 {} 个组织需要刷新", orgList.size());

            int batchSize = 200;
            for (int i = 0; i < orgList.size(); i += batchSize) {
                List<Map<String, String>> batch = orgList.subList(i, Math.min(i + batchSize, orgList.size()));
                refreshBatch(batch);
            }
            log.info("[OrgInfoRefresh] 刷新完成");
        } catch (Exception e) {
            log.error("[OrgInfoRefresh] 刷新异常", e);
        }
    }

    private void refreshBatch(List<Map<String, String>> orgList) {
        for (Map<String, String> entry : orgList) {
            String orgId = entry.get("orgId");
            String orgName = entry.getOrDefault("orgName", "");
            try {
                // SDK服务端按 jobStatusId=15（在职）过滤
                long count = 0L;
                int offset = 0;
                final int pageSize = 500;
                EmpHierarchyCond cond = EmpHierarchyCond.of().jobStatusIdET(15);
                while (true) {
                    Paging paging = new Paging();
                    paging.setOffset(offset);
                    paging.setSize(pageSize);
                    EmpItems empItems = empService.queryEmp(orgId, 0, cond, paging);
                    if (empItems == null || CollectionUtils.isEmpty(empItems.getItems())) break;
                    for (com.sankuai.meituan.org.opensdk.model.domain.Emp emp : empItems.getItems()) {
                        log.debug("[OrgInfoRefresh] orgId={} empId={} name={} jobStatusId={} jobStatus={}", orgId, emp.getEmpId(), emp.getName(), emp.getJobStatusId(), emp.getJobStatus());
                        count++;
                    }
                    if (empItems.getItems().size() < pageSize) break;
                    offset += pageSize;
                }
                log.info("[OrgInfoRefresh] orgId={} orgName={} 在职人数={}", orgId, orgName, count);

                OrgInfo orgInfo = new OrgInfo();
                orgInfo.setOrgId(orgId);
                orgInfo.setOrgName(orgName);
                orgInfo.setEmpCount(count);
                orgInfoDao.upsert(orgInfo);
            } catch (Exception e) {
                log.warn("[OrgInfoRefresh] orgId={} 处理失败，跳过", orgId, e);
            }
        }
    }
}
