package com.example.studentleave.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final StudentAuthInterceptor studentAuthInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        StudentAuthInterceptor studentAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.studentAuthInterceptor = studentAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/**");
        registry.addInterceptor(studentAuthInterceptor)
                .addPathPatterns(
                        "/api/student-home",
                        "/api/active-leave",
                        "/api/leave-records",
                        "/api/leaves/**",
                        "/api/return-requests",
                        "/api/extension-requests");
    }
}
