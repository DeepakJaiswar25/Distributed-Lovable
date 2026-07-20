package com.deepak.distributed_lovable.account_service.security;

import com.deepak.distributed_lovable.common_lib.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class AccountSecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrfConfig -> csrfConfig.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement((sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/webhooks/payments/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandlingConfigurer ->
                        exceptionHandlingConfigurer.accessDeniedHandler((request, response, accessDeniedException) -> {
                            handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
                        }));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}