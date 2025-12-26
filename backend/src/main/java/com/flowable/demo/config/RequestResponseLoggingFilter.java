package com.flowable.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 请求响应日志过滤器
 * 用于包装请求和响应，使得可以多次读取请求体和响应体
 */
@Component
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 包装请求以便多次读取
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // 包装响应以便多次读取
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            // 继续过滤器链
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // 记录请求和响应日志
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, startTime);

            // 确保响应内容被写入原始响应
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        try {
            String characterEncoding = request.getCharacterEncoding();
            String requestBody = new String(request.getContentAsByteArray(), characterEncoding != null ? characterEncoding : StandardCharsets.UTF_8.name());

            log.info("====================================== Incoming Request ===========================================================");
            log.info("Method: {}", request.getMethod());
            log.info("URI: {}", request.getRequestURI());
            log.info("Query String: {}", request.getQueryString());
            //log.info("Remote Address: {}", request.getRemoteAddr());
            //log.info("User-Agent: {}", request.getHeader("User-Agent"));
            //log.info("Content-Type: {}", request.getContentType());
            //log.info("Content-Length: {}", request.getContentLength());

            // 记录请求头
            //log.info("Headers:");
            //request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            //    String headerValue = request.getHeader(headerName);
            //    if (!"Authorization".equalsIgnoreCase(headerName)) {
            //        log.info("  {}: {}", headerName, headerValue);
            //    } else {
            //        log.info("  {}: [REDACTED]", headerName);
            //    }
            //});

            // 记录请求体（仅对非文件上传请求）
            if (request.getContentType() != null && !request.getContentType().contains("multipart/form-data") &&
                    requestBody != null && !requestBody.trim().isEmpty()) {
                log.info("Request Body: {}", requestBody.length() > 1000 ?
                        requestBody.substring(0, 1000) + "...[truncated]" : requestBody);
            }

            log.info("================================================================================================================");

        } catch (Exception e) {
            log.error("Error logging request", e);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            String characterEncoding = response.getCharacterEncoding();
            String responseBody = new String(response.getContentAsByteArray(), characterEncoding != null ? characterEncoding : StandardCharsets.UTF_8.name());

            log.info("============================================== Outgoing Response ===============================================");
            log.info("Status: {}", response.getStatus());
            log.info("Content-Type: {}", response.getContentType());
            log.info("Content-Length: {}", response.getContentSize());
            log.info("Processing Time: {} ms", duration);

            // 记录响应头
            //log.info("Headers:");
            //for (String headerName : response.getHeaderNames()) {
            //    log.info("  {}: {}", headerName, response.getHeader(headerName));
            //}

            // 记录响应体（仅对JSON响应且长度合理）
            if (response.getContentType() != null && response.getContentType().contains("application/json") &&
                    responseBody != null && !responseBody.trim().isEmpty()) {
                log.info("Response Body: {}", responseBody.length() > 2000 ?
                        responseBody.substring(0, 2000) + "...[truncated]" : responseBody);
            }

            log.info("==================================================================================================================");

        } catch (Exception e) {
            log.error("Error logging response", e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // 排除不需要记录日志的路径
        return path.startsWith("/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars") ||
                path.endsWith(".css") ||
                path.endsWith(".js") ||
                path.endsWith(".ico") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg") ||
                path.endsWith(".gif");
    }
}
