package com.example.demo;

import com.example.demo.dto.Auth;
import com.example.demo.dto.Item;
import com.example.demo.dto.ItemDetails;
import com.example.demo.dto.Project;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ApiServiceTest {

    private ClientAndServer mockServer;

    private ApiService apiService;

    private static final ObjectMapper serializer = new ObjectMapper();

    @BeforeEach
    public void setupMockServer() {
        mockServer = ClientAndServer.startClientAndServer(2001);
        apiService = new ApiService(webClientWithMockServerBaseUrl(), new AuthTokenFilter(webClientWithMockServerBaseUrl().build()));
    }

    private WebClient.Builder webClientWithMockServerBaseUrl() {
        return WebClient.builder()
                .baseUrl("http://localhost:" + mockServer.getLocalPort());
    }

    @AfterEach
    public void tearDownServer() {
        mockServer.stop();
    }

    @Test
    public void testTheThing() throws JsonProcessingException {
        mockServer
                .when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath("/auth/token"), once())
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockAuth()));

        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects"))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockProject()));

        mockServer
                .when(request()
                        .withMethod(HttpMethod.GET.name())
                        .withHeader("Authorization", "Bearer test-token")
                        .withPath("/projects/.*"))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value())
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(createMockItemDetails()));

        apiService.getAllData()
                .collectList()
                .block();
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
                        .items(IntStream.range(1, 3)
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
