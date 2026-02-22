package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.store.MoviesStore;
import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            // Пока возвращаем пустой массив (далее будет использоваться store)
            sendJson(ex, 200, "[]");
        } else {
            ex.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }
}