package com.iptiq.loadbalancer;

public interface IPingStrategy {

    boolean[] pingServers(IPing ping, ServiceProvider[] serviceProviders);
}
