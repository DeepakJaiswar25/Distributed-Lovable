package com.deepak.distributed_lovable.api_gateway.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
@Component
public class SecurityProperties {

    private List<String> publicRoutes;
}
