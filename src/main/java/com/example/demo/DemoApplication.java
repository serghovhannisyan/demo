package com.example.demo;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@SpringBootApplication
@Slf4j
public class DemoApplication implements CommandLineRunner {

    public static void main(String[] args) {
        new SpringApplicationBuilder(DemoApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return webClientBuilder -> {
            int timeout = 30;
            int maxMemory = 50;

            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout * 1000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .doOnConnected(conn -> conn
                            .addHandlerLast(new ReadTimeoutHandler(timeout))
                            .addHandlerLast(new WriteTimeoutHandler(timeout)))
                    .responseTimeout(Duration.ofSeconds(timeout));

            webClientBuilder
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(configurer -> configurer.defaultCodecs()
                            .maxInMemorySize(maxMemory * 1024 * 1024))
                    .baseUrl("http://example.com")
                    .build();
        };
    }

    @Autowired
//    @Qualifier(value = "apiService")
    @Qualifier(value = "apiServiceMock")
    private Api api;

    @Override
    public void run(String... args) throws Exception {
        api.getAllData()
                .doOnNext(responseWrapper -> log.info("response {}", responseWrapper))
                .collectList()
                .block();
    }
}
