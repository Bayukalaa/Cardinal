package dev.onlydarkness.utils.core;

import java.util.function.Consumer;

public interface ModuleContext {
    // --- Event Sistemi ---
    void publishEvent(String event, Object payload);
    <T> void subscribe(String topic, Class<T> type, Consumer<T> listener);

    // --- Config Sistemi ---
    String getConfigString(String key, String def);
    int getConfigInt(String key, int def);
    boolean getConfigBoolean(String key, boolean def);

    // --- Komut Sistemi ---
    void registerCommand(ICommand command);

    // --- EKSİK OLAN KISIM: SERVİS SİSTEMİ ---
    // Bu iki satırı mutlaka ekle:
    <T> void registerService(Class<T> serviceClass, T instance);
    <T> T getService(Class<T> serviceClass);
}
