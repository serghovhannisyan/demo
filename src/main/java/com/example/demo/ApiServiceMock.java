package com.example.demo;

import com.example.demo.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceMock implements Api {

    private final WebClient webClient;

    // Entry point for data retrieval
    // There are about 1000 Projects
    // Each project has several Items
    // In order to get ItemDetails we need to create a stream of stream to have all possible Project & Item combinations
    // In the end ResponseWrapper(instead of Tuple) is used to get everything together
    // All requests need token, that's why everything is depended from this call
    // e.g. (project1, item1), (project1, item2), (project2, item1)
    @Override
    public Flux<ResponseWrapper> getAllData() {
        return getToken()
                .doOnNext(auth -> log.info("Token retrieved"))
                .flatMapMany(auth -> {
                    WebClient clientWithToken = getMutatedWebClientWithToken(auth);
                    return getProjects(clientWithToken)
                            .flatMap(project -> Flux.fromIterable(project.getItems())
                                    .flatMap(item -> getItemDetails(clientWithToken, project.getId(), item.getId())
                                            .map(itemDetails -> ResponseWrapper.of(project, item, itemDetails))));
                });
    }

    public Mono<Auth> getToken() {
        return Mono.just(createMockAuth())
                .delayElement(Duration.ofSeconds(1));
    }

    public Flux<Project> getProjects(WebClient client) {
        return Flux.range(1, 10)
                .map(this::createMockProject);
    }

    public Mono<ItemDetails> getItemDetails(WebClient client,
                                            Long projectId,
                                            Long itemId) {
        return Mono.just(createMockItemDetails(projectId, itemId))
                .delayElement(Duration.ofMillis(500));
    }

    private WebClient getMutatedWebClientWithToken(Auth auth) {
        return webClient.mutate()
                .defaultHeader("Authorization", "Bearer " + auth.getToken())
                .build();
    }

    // ============= mock =============
    private Auth createMockAuth() {
        return Auth.builder()
                .token("test-token")
                .build();
    }

    private Project createMockProject(Integer i) {
        return Project.builder()
                .id(Long.valueOf(i))
                .items(IntStream.range(1, 4).
                        mapToObj(value -> new Item((long) value, "name" + value))
                        .collect(Collectors.toList())).build();
    }

    private ItemDetails createMockItemDetails(Long projectId, Long itemId) {
        return new ItemDetails(itemId, "description" + projectId);
    }
}
