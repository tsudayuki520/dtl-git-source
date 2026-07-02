package com.dlust.sportbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(
                        "/api/register/submit",
                        "/api/register/cancel",
                        "/api/my/**",
                        "/api/auth/change-password",
                        // notice 中需要 userId 的端点（@RequestAttribute）需经拦截器注入；
                        // 全局/赛事公告 /api/notice/global、/api/notice/sports-meeting 保持游客可访问
                        "/api/notice/list",
                        "/api/notice/unread-count",
                        "/api/notice/**/read"
                );
    }
}
