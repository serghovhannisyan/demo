package com.example.demo;

import com.example.demo.dto.ResponseWrapper;
import reactor.core.publisher.Flux;

public interface Api {
    // Entry point for data retrieval
    // There are about 1000 Projects
    // Each project has several Items
    // In order to get ItemDetails we need to create a stream of stream to have all possible Project & Item combinations
    // In the end ResponseWrapper(instead of Tuple) is used to get everything together
    // All requests need token, that's why everything is depended from this call
    // e.g. (project1, item1), (project1, item2), (project2, item1)
    Flux<ResponseWrapper> getAllData();
}
