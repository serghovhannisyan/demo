package com.example.demo.filter;

import com.example.demo.filter.model.Filter;
import com.example.demo.filter.model.ItemType;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;

class MyRunnerTest {
    @SneakyThrows
    @Test
    void shouldRun() {
        FilterService filterService = new FilterService();
        MyRunner runner = new MyRunner(new SQSService(), new SNSService(), filterService);

        // change filters from a separate thread
        Flux.interval(Duration.ofMillis(3000))
                .doOnNext(i -> filterService.onFilterChanged(Arrays.asList(new Filter((int) (i % 18), i % 2 == 0 ? ItemType.TYPE_1 : ItemType.TYPE_2))))
                .subscribe();

        runner.run();
    }
}