package com.example.demo.filter;

import com.example.demo.filter.model.Filter;
import com.example.demo.filter.model.Item;
import com.example.demo.filter.model.ItemType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Component
public class FilterService {
    public static final Sinks.EmitFailureHandler RETRY_ON_CONCURRENT_ACCESS_HANDLER =
            (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED;

    private final Map<ItemType, Sinks.Many<Filter>> filtersSink;
    private final Map<ItemType, Mono<Filter>> latestFilterCache;

    public FilterService() {
        this.filtersSink = Arrays.stream(ItemType.values())
                .collect(toMap(type -> type, type -> Sinks.many().multicast().onBackpressureBuffer()));

        // creates a read-only view for the above sinks remembering only the last element
        this.latestFilterCache = filtersSink.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, entry -> createCachedMono(entry.getValue())));
    }

    private Mono<Filter> createCachedMono(Sinks.Many<Filter> sink) {
        return sink.asFlux().cache(1).next();
    }

    public Mono<Void> fetchInitialFilters() {
        return Flux.range(0, 2)
                .map(i -> new Filter(i + 10, i % 2 == 0 ? ItemType.TYPE_1 : ItemType.TYPE_2))
                .doOnNext(filter -> filtersSink.get(filter.getType()).emitNext(filter, RETRY_ON_CONCURRENT_ACCESS_HANDLER))
                .then();
    }

    // reactive redis will receive message and call this method
    public void onFilterChanged(List<Filter> filters) {
        for (Filter filter : filters) {
            System.out.println("new filter: " + filter);
            Sinks.Many<Filter> filterSink = filtersSink.get(filter.getType());
            filterSink.emitNext(filter, RETRY_ON_CONCURRENT_ACCESS_HANDLER);
        }
    }

    public Mono<Boolean> passFilter(Item item) {
        return latestFilterCache.get(item.getType()).map(f -> f.matches(item)).doOnNext(b -> {
            if (!b) {
                System.out.println("Item not passed filter: " + item);
            }
        });
    }
}
