package com.example.demo.exceptions;

public class ProjectsCallException extends RuntimeException {
    public ProjectsCallException(Throwable throwable) {
        super("Projects call failed.", throwable);
    }
}
