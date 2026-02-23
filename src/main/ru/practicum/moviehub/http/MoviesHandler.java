package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if (!path.equals("/movies")) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        switch (method) {
            case "GET":
                handleGet(ex);
                break;
            case "POST":
                handlePost(ex);
                break;
            default:
                ex.sendResponseHeaders(405, -1);
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        List<Movie> movies;

        if (query != null && !query.isEmpty()) {
            Map<String, String> params = parseQuery(query);
            if (params.containsKey("year")) {
                String yearStr = params.get("year");
                try {
                    int year = Integer.parseInt(yearStr);
                    movies = store.getByYear(year);
                } catch (NumberFormatException e) {
                    sendError(ex, 400, "Параметр year должен быть числом");
                    return;
                }
            } else {
                movies = store.getAll();
            }
        } else {
            movies = store.getAll();
        }

        String json = gson.toJson(movies);
        sendJson(ex, 200, json);
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        Movie newMovie;
        try {
            newMovie = gson.fromJson(body, Movie.class);
        } catch (Exception e) {
            sendError(ex, 400, "Некорректный JSON");
            return;
        }

        if (newMovie.getTitle() == null || newMovie.getTitle().isBlank() || newMovie.getYear() <= 0) {
            sendError(ex, 400, "Название не может быть пустым, год должен быть положительным");
            return;
        }

        Movie created = store.add(newMovie);
        String json = gson.toJson(created);
        sendJson(ex, 201, json);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }
}