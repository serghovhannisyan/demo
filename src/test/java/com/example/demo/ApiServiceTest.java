package com.example.demo;

import com.example.demo.dto.Auth;
import com.example.demo.dto.Item;
import com.example.demo.dto.ItemDetails;
import com.example.demo.dto.Project;
import com.example.demo.dto.ResponseWrapper;
import com.example.demo.exceptions.ProjectsCallException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Delay;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

public class ApiServiceTest {

    private ClientAndServer mockServer;

    private ApiService apiService;

    private static final ObjectMapper serializer = new ObjectMapper();

    @BeforeEach
    public void setupMockServer() {
        mockServer = ClientAndServer.startClientAndServer(2001);
        apiService = new ApiService(webClientWithMockServerBaseUrl(), new AuthTokenFilter(webClientWithMockServerBaseUrl(),
                Duration.ofSeconds(1)));
    }

    private WebClient.Builder webClientWithMockServerBaseUrl() {
        return WebClient.builder()
                .baseUrl("http://localhost:" + mockServer.getLocalPort());
    }

    @AfterEach
    public void tearDownServer() {
        mockServer.stop();
    }

    @SneakyThrows
    @Test
    public void testTheThing() throws JsonProcessingException {
        mockSuccessTokenCall();
        mockSuccessProjectsCall();
        mockSuccessItemDetailsCall();

        apiService.getAllData()
                .collectList()
                .block();

        mockServer.verify(request().withPath("/auth/token"), exactly(2));
    }

    @SneakyThrows
    @Test
    public void testFailingItemRetriedAndSuccess() {
        mockSuccessTokenCall();
        mockSuccessProjectsCall();
        mockOneErrorItemDetailsCallForProjectAndItemId(1, 1);
        mockSuccessItemDetailsCall();

        List<ResponseWrapper> responseWrappers = apiService.getAllData()
                .collectList()
                .block();

        mockServer.verify(request().withPath("/projects/1/details/1"), exactly(2));

        // successful after retry
        assertThat(responseWrappers)
                .anyMatch(r -> r.getItem().getId() == 1 && r.getProjectId() == 1);
    }

    @SneakyThrows
    @Test
    public void testItemFailsAfterRetryAndNotIncludedInResponse() {
        mockSuccessTokenCall();
        mockSuccessProjectsCall();

        // fails twice
        mockOneErrorItemDetailsCallForProjectAndItemId(1, 1);
        mockOneErrorItemDetailsCallForProjectAndItemId(1, 1);

        mockSuccessItemDetailsCall();

        List<ResponseWrapper> responseWrappers = apiService.getAllData()
                .collectList()
                .block();

        mockServer.verify(request().withPath("/projects/1/details/1"), exactly(2));

        // not in the output
        assertThat(responseWrappers)
                .noneMatch(r -> r.getItem().getId() == 1 && r.getProjectId() == 1);
    }

    @SneakyThrows
    @Test
    public void testProjectsCallFailureWithMeaningfulException() {
        mockSuccessTokenCall();
        mockErrorProjectsCall();
        mockSuccessItemDetailsCall();

        assertThatThrownBy(() -> apiService.getAllData().collectList().block())
                .isExactlyInstanceOf(ProjectsCallException.class);
    }

    @SneakyThrows
    @Test
    public void testTokenCallIsRetriedAndProcessSucceeds() {
        mockOneErrorTokenCall();
        mockSuccessTokenCall();
        mockSuccessProjectsCall();
        mockSuccessItemDetailsCall();

        List<ResponseWrapper> wrappers = apiService.getAllData().collectList().block();

        // 1: original (fails), 2: retry, 3: new token after expire
        mockServer.verify(request().withPath("/auth/token"), exactly(3));

        assertThat(wrappers).isNotEmpty();
    }

    private void mockSuccessItemDetailsCall() throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects/.*"))
                .respond(response()
                        .withDelay(Delay.milliseconds(1001)) // ensure first round of requests finish after token expires
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockItemDetails()));
    }

    private void mockOneErrorItemDetailsCallForProjectAndItemId(int projectId, int itemId) throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects/" + projectId + "/details/" + itemId), once())
                .respond(response()
                        .withDelay(Delay.milliseconds(1001)) // ensure first round of requests finish after token expires
                        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withContentType(MediaType.APPLICATION_JSON));
    }

    private void mockSuccessProjectsCall() throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects"))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockProject()));
    }

    private void mockErrorProjectsCall() {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects"))
                .respond(response()
                        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withContentType(MediaType.APPLICATION_JSON));
    }

    private void mockSuccessTokenCall() throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath("/auth/token"))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockAuth()));
    }

    private void mockOneErrorTokenCall() throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath("/auth/token"), once())
                .respond(response()
                        .withStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withContentType(MediaType.APPLICATION_JSON));
    }

    private String createMockAuth() throws JsonProcessingException {
        Auth auth = Auth.builder()
                .token("test-token")
                .build();

        return serializer.writeValueAsString(auth);
    }

    private String createMockProject() throws JsonProcessingException {
        return serializer.writeValueAsString(IntStream.range(1, 5)
                .mapToLong(Long::valueOf)
                .mapToObj(i -> Project.builder()
                        .id(i)
                        // by default flatmap concurrency is 256,
                        // adding more items here ensures that some requests have to wait for one of the previous ones to finish
                        // which makes it possible to test token expiration
                        .items(IntStream.rangeClosed(1, 300)
                                .mapToLong(Long::valueOf)
                                .mapToObj(j -> new Item(j, "name" + j))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList()));
    }

    private String createMockItemDetails() throws JsonProcessingException {
        return serializer.writeValueAsString(new ItemDetails(555L, "test-description"));
    }

}
