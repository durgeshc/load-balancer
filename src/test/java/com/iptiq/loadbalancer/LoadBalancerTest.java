package com.iptiq.loadbalancer;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoadBalancerTest {
    private int testLimit = 10; // This should be greater than chunk size
    private List<ServiceProvider> serviceProviders;
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerTest.class);

    @Before
    public void setup() {
         serviceProviders = IntStream.range(0,testLimit).mapToObj((id) -> new ServiceProvider(String.valueOf(id))).collect(Collectors.toList());
    }

    @Test
    public void testDefaultLoadBalancerRoundRobin() throws Exception {
        ILoadBalancer lb = new DefaultLoadBalancer();
        lb.addServers(serviceProviders);
        for(int i = 0; i < testLimit*100; i++) {
            log.debug("Result for Req [{}] from server with id [{}]", i, lb.get(null));
        }
    }

    @Test
    public void testDefaultLoadBalancerRandom() throws Exception {
        ILoadBalancer lb = new DefaultLoadBalancer();
        lb.setLoadBalancingAlgorithm(new RandomLoadBalancing(lb));
        lb.addServers(serviceProviders);
        for(int i = 0;i<testLimit*10;i++) {
            log.debug("Result for Req [{}] from server with id [{}]", i, lb.get(null));
        }

    }


    @Test
    public void testLimitOfServers() throws Exception {
        ILoadBalancer lb = new DefaultLoadBalancer();
        lb.setLoadBalancingAlgorithm(new RandomLoadBalancing(lb));
        serviceProviders.add(new ServiceProvider(String.valueOf(testLimit + 1)));
        lb.addServers(serviceProviders);
    }
}
