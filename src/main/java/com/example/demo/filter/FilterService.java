package com.example.demo.filter;

import com.example.demo.filter.model.Filter;
import com.example.demo.filter.model.Item;
import com.example.demo.filter.model.ItemType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class FilterService {

    private final Mono<Map<ItemType, Filter>> filterCache;

    public FilterService() {
        this.filterCache = fetchFilters()
                .cache(v -> Duration.ofMinutes(60), e -> Duration.ZERO, () -> Duration.ZERO);
    }

    public Mono<Map<ItemType, Filter>> fetchFilters() {
        return Flux.range(0, 2)
                .map(i -> new Filter(i + 10, i % 2 == 0 ? ItemType.TYPE_1 : ItemType.TYPE_2))
                .collectMap(Filter::getType);
    }

    // reactive redis will receive message and call this method
    public void onFilterChanged(List<Filter> filters) {
        // TODO: how to update filterCache?
    }

    public Mono<Boolean> passFilter(Item item) {
        return filterCache.map(filterMap -> filterMap.get(item.getType()).matches(item));
    }
}
