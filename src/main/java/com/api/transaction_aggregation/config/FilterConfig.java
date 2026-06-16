package com.api.transaction_aggregation.config;

import com.api.transaction_aggregation.auth.filter.JwtAuthFilter;
import com.api.transaction_aggregation.auth.service.ApiTokenService;
import com.api.transaction_aggregation.auth.service.JwtService;
import com.api.transaction_aggregation.auth.session.SessionCache;
import com.api.transaction_aggregation.ratelimit.RateLimitCache;
import com.api.transaction_aggregation.ratelimit.RateLimitFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilter(JwtService jwtService,
                                                            ApiTokenService apiTokenService,
                                                            SessionCache sessionCache) {
        FilterRegistrationBean<JwtAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtAuthFilter(jwtService, apiTokenService, sessionCache));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RateLimitCache rateLimitCache) {
        FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(rateLimitCache));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
}
