package com.iptiq.loadbalancer;

import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancing implements ILoadBalancingAlgorithm {

    private AtomicInteger nextServerCyclicCounter;

    private ILoadBalancer lb;

    private static org.slf4j.Logger log = LoggerFactory.getLogger(RoundRobinLoadBalancing.class);

    public RoundRobinLoadBalancing(ILoadBalancer lb) {
        nextServerCyclicCounter = new AtomicInteger(0);
        this.lb = lb;
    }

    @Override
    public ServiceProvider choose() {
        if (lb == null) {
            log.warn("no load balancer");
            return null;
        }

        ServiceProvider serviceProvider = null;
        int count = 0;
        while (serviceProvider == null && count++ < 10) {
            List<ServiceProvider> reachableServiceProviders = lb.getReachableServers();
            List<ServiceProvider> allServiceProviders = lb.getAllServers();
            int upCount = reachableServiceProviders.size();
            int serverCount = allServiceProviders.size();

            if ((upCount == 0) || (serverCount == 0)) {
                log.warn("No up servers available from load balancer: " + lb);
                return null;
            }

            int nextServerIndex = incrementAndGetModulo(serverCount);
            serviceProvider = allServiceProviders.get(nextServerIndex);

            if (serviceProvider == null) {
                /* Transient. */
                Thread.yield();
                continue;
            }

            if (serviceProvider.isAlive() && (serviceProvider.isReadyToServe())) {
                return (serviceProvider);
            }

            // Next.
            serviceProvider = null;
        }

        if (count >= 10) {
            log.warn("No available alive servers after 10 tries from load balancer: "
                    + lb);
        }
        return serviceProvider;
    }

    /**
     * Inspired by the implementation of {@link AtomicInteger#incrementAndGet()}.
     *
     * @param modulo The modulo to bound the value of the counter.
     * @return The next value.
     */
    private int incrementAndGetModulo(int modulo) {
        for (;;) {
            int current = nextServerCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextServerCyclicCounter.compareAndSet(current, next))
                return next;
        }
    }





}
