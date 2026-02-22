package ru.practicum.moviehub.api;

import java.util.List;
import java.util.Objects;

public class ErrorResponse {
    private String error;
    private List<String> details;

    public ErrorResponse() {}

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(error, that.error) && Objects.equals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, details);
    }
}