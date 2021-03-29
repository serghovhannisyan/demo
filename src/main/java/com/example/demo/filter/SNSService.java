package com.example.demo.filter;

import com.example.demo.filter.model.Item;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class SNSService {

    public Mono<Void> saveItems(Item item) {
        return Mono.just("")
                .delayElement(Duration.ofSeconds(1))
                .then();
    }
}
