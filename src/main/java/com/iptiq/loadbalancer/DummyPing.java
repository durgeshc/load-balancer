package com.iptiq.loadbalancer;

public class DummyPing implements IPing {
    public DummyPing() {
    }

    public boolean isAlive(ServiceProvider serviceProvider) {
        return serviceProvider.check();
    }
}
