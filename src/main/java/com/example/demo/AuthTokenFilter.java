package com.example.demo;

import com.example.demo.dto.Auth;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class AuthTokenFilter implements ExchangeFilterFunction {
    private final WebClient webClient;
    private final Mono<Auth> authProvider;

    public AuthTokenFilter(WebClient webClient) {
        this.webClient = webClient;

        // cache operator ensures that subsequent calls can reuse existing token, you can even set a TTL if the token can expire
        this.authProvider = authenticate().cache();
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
