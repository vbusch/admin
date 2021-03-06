package enmasse.controller.api;

import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.*;

public class TestInstanceManager implements InstanceManager {
    Map<InstanceId, Instance> instances = new HashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public Optional<Instance> get(String uuid) {
        for (Instance i : instances.values()) {
            if (i.uuid().filter(u -> uuid.equals(u)).isPresent()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public void create(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.put(instance.id(), instance);
    }

    @Override
    public void delete(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.remove(instance.id());
    }

    @Override
    public Set<Instance> list() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(instances.values());
    }
}
