package tizio.dev.tsp.core.client;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
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

    public Set<ResourceLocation> keySet() {
        return instances.keySet();
    }

    public void retainAll(Set<ResourceLocation> keepKeys) {
        instances.keySet().retainAll(keepKeys);
    }
}
