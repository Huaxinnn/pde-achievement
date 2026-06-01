package com.meituan.pde.config;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@WebFilter(urlPatterns = "/ulivepde/*", filterName = "requestLogFilter", asyncSupported = true)
public class RequestLogFilter implements Filter {

    private static final Set<String> BODY_METHODS = new HashSet<>(Arrays.asList("POST", "PUT", "PATCH"));
    private static final int MAX_BODY_LOG_LENGTH = 1000;
    // SSE 接口不记录响应（长连接）
    private static final String SSE_PATH = "/ulivepde/api/activity/stream";

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String queryString = httpRequest.getQueryString();
        String url = uri + (queryString != null ? "?" + queryString : "");
        String mis = getMis(httpRequest);
        long startTime = System.currentTimeMillis();

        // POST/PUT/PATCH 需要包装 request 以支持多次读取 body
        if (BODY_METHODS.contains(method)) {
            BodyReaderRequestWrapper wrappedRequest = new BodyReaderRequestWrapper(httpRequest);
            String body = wrappedRequest.getBodyString();
            if (body.length() > MAX_BODY_LOG_LENGTH) {
                body = body.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)";
            }
            log.info("[REQ] {} {} mis={} body={}", method, url, mis, body);
            try {
                chain.doFilter(wrappedRequest, response);
            } finally {
                if (!uri.contains(SSE_PATH)) {
                    log.info("[RES] {} {} mis={} status={} cost={}ms",
                            method, url, mis, httpResponse.getStatus(),
                            System.currentTimeMillis() - startTime);
                }
            }
        } else {
            log.info("[REQ] {} {} mis={}", method, url, mis);
            try {
                chain.doFilter(request, response);
            } finally {
                if (!uri.contains(SSE_PATH)) {
                    log.info("[RES] {} {} mis={} status={} cost={}ms",
                            method, url, mis, httpResponse.getStatus(),
                            System.currentTimeMillis() - startTime);
                }
            }
        }
    }

    private String getMis(HttpServletRequest request) {
        String mockMis = request.getParameter("mock_mis");
        if (mockMis != null && !mockMis.isEmpty()) {
            return mockMis;
        }
        try {
            com.sankuai.meituan.auth.vo.User user = com.sankuai.meituan.auth.util.UserUtils.getUser();
            if (user != null && user.getLogin() != null) {
                return user.getLogin();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
