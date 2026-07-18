package com.habitiq.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            String ip;
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                ip = forwardedFor.split(",")[0].trim();
            } else {
                var remoteAddress = exchange.getRequest().getRemoteAddress();
                ip = (remoteAddress != null)
                        ? remoteAddress.getAddress().getHostAddress()
                        : "unknown";
            }

            return Mono.just(ip);
        };
    }
}
