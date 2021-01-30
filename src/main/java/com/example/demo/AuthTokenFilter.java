package com.example.demo;

import com.example.demo.dto.Auth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class AuthTokenFilter implements ExchangeFilterFunction {
    private final WebClient webClient;
    private final Mono<Auth> authProvider;

    public AuthTokenFilter(WebClient.Builder webClientBuilder, @Value("${token.expire}") Duration tokenExpire) {
        this.webClient = webClientBuilder.build();

        // cache operator ensures that subsequent calls can reuse existing token, you can even set a TTL if the token can expire
        this.authProvider = authenticate()
                .retryWhen(Retry.backoff(1, Duration.ofSeconds(1)))
                .cache(x -> tokenExpire, ex -> Duration.ZERO, () -> Duration.ZERO); // don't cache error or empty
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return authProvider
                .map(auth -> ClientRequest.from(request)
                        .headers(headers -> headers.setBearerAuth(auth.getToken()))
                        .build())
                .flatMap(next::exchange);
    }

    private Mono<Auth> authenticate() {
        return webClient.post()
                .uri("/auth/token")
                .body(BodyInserters.fromFormData("username", "test-user")
                        .with("password", "test-password"))
                .retrieve()
                .bodyToMono(Auth.class);
    }
}
