package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

public class MovieHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public MovieHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if (!path.startsWith("/movies/")) {
            ex.sendResponseHeaders(404, -1);
            return;
        }

        String idStr = path.substring("/movies/".length());
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            sendError(ex, 400, "ID должен быть числом");
            return;
        }

        switch (method) {
            case "GET":
                handleGet(ex, id);
                break;
            case "DELETE":
                handleDelete(ex, id);
                break;
            default:
                ex.sendResponseHeaders(405, -1);
        }
    }

    private void handleGet(HttpExchange ex, int id) throws IOException {
        Optional<Movie> movie = store.getById(id);
        if (movie.isPresent()) {
            String json = gson.toJson(movie.get());
            sendJson(ex, 200, json);
        } else {
            sendError(ex, 404, "Фильм с id " + id + " не найден");
        }
    }

    private void handleDelete(HttpExchange ex, int id) throws IOException {
        boolean deleted = store.delete(id);
        if (deleted) {
            sendNoContent(ex);
        } else {
            sendError(ex, 404, "Фильм с id " + id + " не найден");
        }
    }
}