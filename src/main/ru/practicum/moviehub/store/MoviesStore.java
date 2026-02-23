package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> storage = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public List<Movie> getAll() {
        return new ArrayList<>(storage.values());
    }

    public Movie add(Movie movie) {
        movie.setId(idGenerator.getAndIncrement());
        storage.put(movie.getId(), movie);
        return movie;
    }

    public Optional<Movie> getById(int id) {
        return Optional.ofNullable(storage.get(id));
    }

    public boolean delete(int id) {
        return storage.remove(id) != null;
    }

    public List<Movie> getByYear(int year) {
        return storage.values().stream()
                .filter(m -> m.getYear() == year)
                .collect(Collectors.toList());
    }

    public void clear() {
        storage.clear();
        idGenerator.set(1);
    }
}