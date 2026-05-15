package tizio.dev.engine.instance;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientRenderInstanceRegistry<T> {

    private final String defaultNamespace;
    private final Map<ResourceLocation, T> instances = new ConcurrentHashMap<>();

    public ClientRenderInstanceRegistry(String defaultNamespace) {
        this.defaultNamespace = Objects.requireNonNull(defaultNamespace, "defaultNamespace");
    }

    public ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(defaultNamespace, path);
    }

    public void put(ResourceLocation id, T instance) {
        instances.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(instance, "instance"));
    }

    public void put(String path, T instance) {
        put(id(path), instance);
    }

    public T get(ResourceLocation id) {
        return instances.get(id);
    }

    public T get(String path) {
        return get(id(path));
    }

    public void remove(ResourceLocation id) {
        instances.remove(id);
    }

    public void remove(String path) {
        remove(id(path));
    }

    public void clear() {
        instances.clear();
    }

    public boolean isEmpty() {
        return instances.isEmpty();
    }

    public int size() {
        return instances.size();
    }

    public Collection<T> instances() {
        return Collections.unmodifiableCollection(instances.values());
    }
}
