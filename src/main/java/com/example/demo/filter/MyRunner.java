package com.example.demo.filter;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class MyRunner {

    private final SQSService sqsService;
    private final SNSService snsService;
    private final FilterService filterService;

    public MyRunner(SQSService sqsService, SNSService snsService, FilterService filterService) {
        this.sqsService = sqsService;
        this.snsService = snsService;
        this.filterService = filterService;
    }

    // this will be called during startup and run forever
    public void run() {
        filterService.fetchInitialFilters()
                .thenMany(sqsService.getItems()
                        .retry() // read from queue even on errors
                        .repeat()) // read from queue all the time
                .flatMap(Flux::fromIterable)
                .doOnNext(item -> System.out.println("Starting to process item: " + item))
                .filterWhen(filterService::passFilter)
                .flatMap(snsService::saveItems)
                .collectList()
                .block();
    }

    // TODO:
    public void run2() {
        // fetch filters? be sure we have it? (in further make sure they are updated on Redis Pub/Sub)
        // start polling SQS
        // getFilterByType
        // push valid items into SNS
    }
}
