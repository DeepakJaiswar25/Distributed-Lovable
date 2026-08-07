package com.deepak.distributed_lovable.common_lib.security;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.HandlerExceptionResolver;

@AutoConfiguration
@Slf4j
public class SharedSecurityAutoConfiguration {

    @Bean
    public AuthUtil authUtil() {
        return new AuthUtil();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(AuthUtil authUtil,
                                      RequestContext requestContext,
                                      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        return new JwtAuthFilter(authUtil, handlerExceptionResolver, requestContext);
    }

    @Bean
    public RequestContext requestContext() {
        return new RequestContext();
    }

    @Bean
    public RequestInterceptor requestInterceptor(RequestContext requestContext) {
        return requestTemplate -> {
            log.info("Thread: {}", Thread.currentThread().getName());
            log.info("RequestContext JWT: {}", requestContext.getJwt());

            if (requestContext.getJwt() != null && !requestContext.getJwt().isBlank()) {
                log.info("Adding Authorization header");
                requestTemplate.header("Authorization", "Bearer " + requestContext.getJwt());
            } else {
                log.error("JWT is NULL");
            }
        };
    }

}
