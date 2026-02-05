package dev.onlydarkness.utils.core;

public interface IModule {
    String getName();
    void onEnable(ModuleContext context);
    void onDisable();
}
