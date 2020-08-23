package com.iptiq.loadbalancer;

public interface ILoadBalancingAlgorithm {
    ServiceProvider choose();
}