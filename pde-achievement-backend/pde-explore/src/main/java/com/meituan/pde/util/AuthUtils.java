package com.meituan.pde.util;

import com.meituan.pde.common.UnauthorizedException;
import com.sankuai.meituan.auth.util.UserUtils;
import com.sankuai.meituan.auth.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

@Slf4j
public class AuthUtils {

    private AuthUtils() {}

    /**
     * 优先从 SSO ThreadLocal 获取当前登录用户 mis；
     * 本地 localhost 联调时允许通过 mock_mis/X-Mock-Mis 注入调试用户。
     *
     * @throws UnauthorizedException SSO 未生效且未提供本地调试用户时抛出
     */
    public static String getMisFromRequest(HttpServletRequest request) {
        try {
            User user = UserUtils.getUser();
            if (user != null && StringUtils.hasText(user.getLogin())) {
                return user.getLogin();
            }
        } catch (Exception e) {
            log.warn("UserUtils.getUser() 异常: {}", e.getMessage());
        }

        if (isLocalDebugRequest(request)) {
            String mockMis = request.getParameter("mock_mis");
            if (!StringUtils.hasText(mockMis)) {
                mockMis = request.getHeader("X-Mock-Mis");
            }
            if (StringUtils.hasText(mockMis)) {
                return mockMis.trim();
            }
        }

        throw new UnauthorizedException("未登录或 SSO 认证失败");
    }

    private static boolean isLocalDebugRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String serverName = request.getServerName();
        return "localhost".equalsIgnoreCase(serverName)
                || "127.0.0.1".equals(serverName)
                || "::1".equals(serverName);
    }
}
