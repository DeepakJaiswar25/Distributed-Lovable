package com.deepak.distributed_lovable.common_lib.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.context.annotation.ScopedProxyMode;

@Component
@Getter
@Setter
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    private String jwt;
}
