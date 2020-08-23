package com.iptiq.loadbalancer;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ServiceProvider implements IProvider {
    private volatile String id;
    private volatile boolean isAlive;
    private volatile boolean isReadyToServe = true;

    public ServiceProvider(){
        this.id = UUID.randomUUID().toString();
        isAlive = false;
    }

    public ServiceProvider(String id){
        this.id = id;
        isAlive = false;
    }

    public String getId() {
        return id;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public boolean isReadyToServe() {
        return isReadyToServe;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ServiceProvider))
            return false;
        ServiceProvider svc = (ServiceProvider) obj;
        return svc.getId().equals(this.getId());

    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (null == this.getId() ? 0 : this.getId().hashCode());
        return hash;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }

    @Override
    public <T> String get(T req) {
        return id;
    }

    @Override
    public boolean check() {
        // Roughly 90% of the times the server will be up :)
        return ThreadLocalRandom.current().nextInt(0, 10 + 1) < 10 ? true : false;
    }
}