package com.example.demo.filter;

import com.example.demo.filter.model.Item;
import com.example.demo.filter.model.ItemType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class SQSService {

    public Mono<List<Item>> getItems() {
        return Flux.range(0, 10)
                .map(i -> new Item(i, i % 2 == 0 ? ItemType.TYPE_1 : ItemType.TYPE_2, i * 2))
                .collectList()
                .delayElement(Duration.ofSeconds(1));
    }
}
