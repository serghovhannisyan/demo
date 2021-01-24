package com.example.demo;

import com.example.demo.dto.ResponseWrapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class AwsService {

    public Mono<String> saveSQSBatch(List<ResponseWrapper> list) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return "Result of the asynchronous response";
        });

        return Mono.fromFuture(future);
    }

    public Mono<String> saveS3(List<ResponseWrapper> list) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return "Result of the asynchronous response";
        });

        return Mono.fromFuture(future);
    }
}
