package com.example.demo;

import com.example.demo.dto.Auth;
import com.example.demo.dto.ItemDetails;
import com.example.demo.dto.Project;
import com.example.demo.dto.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ApiService implements Api {

    private final WebClient webClient;

    public ApiService(WebClient.Builder clientBuilder, AuthTokenFilter authTokenFilter) {
        this.webClient = clientBuilder.filter(authTokenFilter).build();
    }

    // Entry point for data retrieval
    // There are about 1000 Projects
    // Each project has several Items
    // In order to get ItemDetails we need to create a stream of stream to have all possible Project & Item combinations
    // In the end ResponseWrapper(instead of Tuple) is used to get everything together
    // All requests need token, that's why everything is depended from this call
    // e.g. (project1, item1), (project1, item2), (project2, item1)
    @Override
    public Flux<ResponseWrapper> getAllData() {
        return getProjects()
                .flatMap(project -> Flux.fromIterable(project.getItems())
                        .flatMap(item -> getItemDetails(project.getId(), item.getId())
                                .map(itemDetails -> ResponseWrapper.of(project, item, itemDetails))));
    }

    public Flux<Project> getProjects() {
        return webClient.get()
                .uri("/projects")
                .retrieve()
                .bodyToFlux(Project.class);
    }

    public Mono<ItemDetails> getItemDetails(Long projectId,
                                            Long itemId) {
        return webClient.get()
                .uri("/projects/{projectId}/details/{itemId}", projectId, itemId)
                .retrieve()
                .bodyToMono(ItemDetails.class);
    }
}
