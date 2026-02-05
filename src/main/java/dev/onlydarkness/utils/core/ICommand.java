package dev.onlydarkness.utils.core;

public interface ICommand {

    String getName();
    String getDescription();

    void execute(String[] args);
}
