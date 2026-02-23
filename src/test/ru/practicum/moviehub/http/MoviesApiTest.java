package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final Gson gson = new Gson();
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPost(String path, Object body) throws Exception {
        String jsonBody = gson.toJson(body);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDelete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = sendGet("/movies");

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));

        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenNotEmpty_returnsList() throws Exception {
        Movie m1 = store.add(new Movie("Inception", 2010));
        Movie m2 = store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = sendGet("/movies");

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().anyMatch(m -> m.getId() == m1.getId() && m.getTitle().equals("Inception")));
        assertTrue(movies.stream().anyMatch(m -> m.getId() == m2.getId() && m.getTitle().equals("Interstellar")));
    }

    @Test
    void postMovie_whenValid_returnsCreatedAndMovie() throws Exception {
        Movie newMovie = new Movie("The Matrix", 1999);
        HttpResponse<String> resp = sendPost("/movies", newMovie);

        assertEquals(201, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));

        Movie created = gson.fromJson(resp.body(), Movie.class);
        assertNotNull(created.getId());
        assertEquals("The Matrix", created.getTitle());
        assertEquals(1999, created.getYear());

        Movie fromStore = store.getById(created.getId()).orElse(null);
        assertNotNull(fromStore);
        assertEquals(created.getId(), fromStore.getId());
    }

    @Test
    void postMovie_whenInvalidJson_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString("not a json"))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный JSON", error.getError());
    }

    @Test
    void postMovie_whenMissingTitle_returns400() throws Exception {
        Movie invalid = new Movie();
        invalid.setYear(2000);
        HttpResponse<String> resp = sendPost("/movies", invalid);

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Название не может быть пустым, год должен быть положительным", error.getError());
    }

    @Test
    void postMovie_whenNegativeYear_returns400() throws Exception {
        Movie invalid = new Movie("Bad", -5);
        HttpResponse<String> resp = sendPost("/movies", invalid);

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Название не может быть пустым, год должен быть положительным", error.getError());
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie movie = store.add(new Movie("Pulp Fiction", 1994));
        HttpResponse<String> resp = sendGet("/movies/" + movie.getId());

        assertEquals(200, resp.statusCode());
        Movie found = gson.fromJson(resp.body(), Movie.class);
        assertEquals(movie.getId(), found.getId());
        assertEquals("Pulp Fiction", found.getTitle());
        assertEquals(1994, found.getYear());
    }

    @Test
    void getMovieById_whenNotExists_returns404() throws Exception {
        HttpResponse<String> resp = sendGet("/movies/999");

        assertEquals(404, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм с id 999 не найден", error.getError());
    }

    @Test
    void getMovieById_whenInvalidIdFormat_returns400() throws Exception {
        HttpResponse<String> resp = sendGet("/movies/abc");

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("ID должен быть числом", error.getError());
    }

    @Test
    void deleteMovieById_whenExists_returns204() throws Exception {
        Movie movie = store.add(new Movie("Test", 2020));
        HttpResponse<String> resp = sendDelete("/movies/" + movie.getId());

        assertEquals(204, resp.statusCode());
        assertTrue(resp.body().isEmpty());
        assertFalse(store.getById(movie.getId()).isPresent());
    }

    @Test
    void deleteMovieById_whenNotExists_returns404() throws Exception {
        HttpResponse<String> resp = sendDelete("/movies/999");

        assertEquals(404, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Фильм с id 999 не найден", error.getError());
    }

    @Test
    void deleteMovieById_whenInvalidIdFormat_returns400() throws Exception {
        HttpResponse<String> resp = sendDelete("/movies/xyz");

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("ID должен быть числом", error.getError());
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
    }


    @Test
    void getMovies_withYearFilter_returnsFilteredList() throws Exception {
        store.add(new Movie("Inception", 2010));
        store.add(new Movie("Interstellar", 2014));
        store.add(new Movie("The Dark Knight", 2008));

        HttpResponse<String> resp = sendGet("/movies?year=2010");

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(1, movies.size());
        assertEquals("Inception", movies.get(0).getTitle());
    }

    @Test
    void getMovies_withYearFilterNoMatches_returnsEmptyArray() throws Exception {
        store.add(new Movie("Inception", 2010));
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = sendGet("/movies?year=1999");

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_withInvalidYearFormat_returns400() throws Exception {
        HttpResponse<String> resp = sendGet("/movies?year=abc");

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Параметр year должен быть числом", error.getError());
    }

    @Test
    void getMovies_withExtraQueryParams_ignoresThemAndReturnsAll() throws Exception {
        store.add(new Movie("Inception", 2010));
        store.add(new Movie("Interstellar", 2014));

        HttpResponse<String> resp = sendGet("/movies?foo=bar&year=2010&baz=qux");

        assertEquals(200, resp.statusCode());
        List<Movie> movies = gson.fromJson(resp.body(), new TypeToken<List<Movie>>(){}.getType());
        assertEquals(1, movies.size()); // параметр year распознан, другие игнорируются
        assertEquals("Inception", movies.get(0).getTitle());
    }
}