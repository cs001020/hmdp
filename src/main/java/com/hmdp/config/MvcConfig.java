package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "user/login",
                        "user/code",
                        "blog/hot",
                        "shop/**",
                        "shop-type/**",
                        "voucher/**",
                        "upload/**"
                );
    }
}
