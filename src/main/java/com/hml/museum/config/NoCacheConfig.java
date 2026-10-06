package com.hml.museum.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class NoCacheConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response, Object handler) {
                if ("GET".equalsIgnoreCase(request.getMethod())) {
                    response.setHeader(HttpHeaders.CACHE_CONTROL,
                            "no-cache, no-store, must-revalidate");
                    response.setHeader(HttpHeaders.PRAGMA, "no-cache");
                    response.setHeader(HttpHeaders.EXPIRES, "0");
                }
                return true;
            }
        }).addPathPatterns("/**");
    }
}