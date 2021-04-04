package com.example.demo.filter;

import com.example.demo.filter.model.Filter;
import com.example.demo.filter.model.ItemType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;

class FilterServiceTest {

    @Test
    void shouldNotFailOnConcurrentUpdate() {
        FilterService filterService = new FilterService();
        filterService.fetchInitialFilters().block();

        Flux.range(1, 5000)
                .flatMap(a -> Mono.fromRunnable(() -> filterService.onFilterChanged(Arrays.asList(new Filter(1, ItemType.TYPE_1))))
                        .subscribeOn(Schedulers.parallel()), 1000)
                .blockLast();
    }
}