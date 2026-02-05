package dev.onlydarkness.utils;

import dev.onlydarkness.utils.commands.*;
import dev.onlydarkness.utils.core.ICommand;
import dev.onlydarkness.utils.core.IModule;
import dev.onlydarkness.utils.core.ModuleContext;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Consumer;

public class ModuleManager implements ModuleContext {

    private final List<IModule> loadedModules = new ArrayList<>();
    private final Map<String, String> failedModules = new HashMap<>();


    private final Map<Class<?>, Object> services = new HashMap<>();

    private final Map<String, List<Consumer<Object>>> subscribers = new HashMap<>();
    private final ConfigManager configManager;
    private final CommandManager commandManager;

    private static final String MODULES_FOLDER = "modules";

    public ModuleManager() {
        this.configManager = new ConfigManager("server.properties");
        this.commandManager = new CommandManager();

        registerDefaultCommands();
    }

    public void dispatchCommand(String input) {
        publishEvent("LOG_INPUT", "> " + input);
        commandManager.dispatch(input);
    }

    public void reloadConfiguration() {
        configManager.reloadConfig();
        publishEvent("SYSTEM_LOG", "Configuration reloaded. Some changes may require a hard reload.");
    }

    public void performHardReload(){
        long start = System.currentTimeMillis();
        System.out.println("[Cardinal] --- STARTING HARD RELOAD ---");
        System.out.println("[Cardinal] Reloading configuration...");

        unload();

        configManager.reloadConfig();
        load();
        long duration = System.currentTimeMillis() - start;
        System.out.println("[Cardinal] --- HARD RELOAD COMPLETE (" + duration + "ms) ---");

        publishEvent("SYSTEM_LOG", "System hard reload completed in " + duration + "ms.");
    }

    public void load() {
        System.out.println("[ModuleManager] Scanning for modules...");

        try {
            File folder = new File(MODULES_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
                System.out.println("[ModuleManager] Created '" + MODULES_FOLDER + "' directory.");
            }

            List<URL> jarUrls = new ArrayList<>();
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));

            if (files != null) {
                for (File file : files) {
                    jarUrls.add(file.toURI().toURL());
                    System.out.println("[ModuleManager] Found external JAR: " + file.getName());
                }
            }

            URLClassLoader moduleClassLoader = new URLClassLoader(
                    jarUrls.toArray(new URL[0]),
                    this.getClass().getClassLoader()
            );

            ServiceLoader<IModule> loader = ServiceLoader.load(IModule.class, moduleClassLoader);

            for (IModule module : loader) {
                try {
                    if (isModuleLoaded(module.getName())) {
                        System.out.println("[ModuleManager] Warning: Module '" + module.getName() + "' is already loaded. Skipping.");
                        continue;
                    }

                    module.onEnable(this);

                    loadedModules.add(module);
                    System.out.println("[ModuleManager] Loaded: " + module.getName());

                } catch (Throwable e) {
                    String moduleName = module.getClass().getSimpleName();
                    try { moduleName = module.getName(); } catch (Exception ignored) {}

                    System.err.println("[ModuleManager] ERROR: Failed to load " + moduleName);
                    e.printStackTrace();

                    failedModules.put(moduleName, e.getMessage());
                }
            }

        } catch (MalformedURLException e) {
            System.err.println("[ModuleManager] URL Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ModuleManager] Critical Error during loading: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[ModuleManager] Active: " + loadedModules.size() + " | Failed: " + failedModules.size());
    }

    public void unload() {
        System.out.println("[ModuleManager] Unloading modules...");

        ListIterator<IModule> iterator = loadedModules.listIterator(loadedModules.size());
        while (iterator.hasPrevious()) {
            IModule module = iterator.previous();
            try {
                module.onDisable();
            } catch (Exception e) {
                System.err.println("[ModuleManager] Error disabling " + module.getName() + ": " + e.getMessage());
            }
        }

        loadedModules.clear();
        failedModules.clear();
        services.clear();
        subscribers.clear();
        System.out.println("[ModuleManager] All modules unloaded.");
    }

    private boolean isModuleLoaded(String name) {
        return loadedModules.stream().anyMatch(m -> m.getName().equals(name));
    }

    // --- MODULE CONTEXT IMPLEMENTASYONLARI ---

    @Override
    public void publishEvent(String event, Object payload) {
        if (subscribers.containsKey(event)) {
            new ArrayList<>(subscribers.get(event)).forEach(listener -> {
                try {
                    listener.accept(payload);
                } catch (Exception e) {
                    System.err.println("[EventBus] Error in listener for '" + event + "': " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public String getConfigString(String key, String def) {
        return configManager.getString(key, def);
    }

    @Override
    public int getConfigInt(String key, int def) {
        return configManager.getInt(key, def);
    }

    @Override
    public boolean getConfigBoolean(String key, boolean def) {
        return configManager.getBoolean(key, def);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void subscribe(String topic, Class<T> type, Consumer<T> listener) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(obj -> {
            if (type.isInstance(obj)) {
                listener.accept(type.cast(obj));
            }
        });
    }

    @Override
    public void registerCommand(ICommand command){
        commandManager.register(command);
    }



    @Override
    public <T> void registerService(Class<T> serviceClass, T instance) {
        services.put(serviceClass, instance);

         System.out.println("[Service] Registered: " + serviceClass.getSimpleName());
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        Object service = services.get(serviceClass);
        if (service != null && serviceClass.isInstance(service)) {
            return serviceClass.cast(service);
        }
        return null;
    }



    private void registerDefaultCommands() {
        registerCommand(new StopCommand());
        registerCommand(new ReloadCommand(this));
        registerCommand(new HardReloadCommand(this));
        registerCommand(new HelpCommand(this.commandManager));
        registerCommand(new ModulesCommand(this));
    }

    public List<IModule> getLoadedModules() {
        return Collections.unmodifiableList(loadedModules);
    }

    public Map<String, String> getFailedModules() {
        return Collections.unmodifiableMap(failedModules);
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}