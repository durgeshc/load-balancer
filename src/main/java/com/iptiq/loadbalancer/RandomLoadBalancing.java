package com.iptiq.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLoadBalancing implements ILoadBalancingAlgorithm {

    /**
     * Randomly choose from all living servers
     */
    private ILoadBalancer lb;
    private static final Logger log = LoggerFactory.getLogger(RandomLoadBalancing.class);
    public RandomLoadBalancing(ILoadBalancer lb) {
        this.lb = lb;
    }
    public ServiceProvider choose() {
        if (lb == null) {
            return null;
        }
        ServiceProvider serviceProvider = null;

        while (serviceProvider == null) {
            if (Thread.interrupted()) {
                return null;
            }
            List<ServiceProvider> upList = lb.getReachableServers();
            List<ServiceProvider> allList = lb.getAllServers();

            int serverCount = allList.size();
            if (serverCount == 0) {
                /*
                 * No servers. End regardless of pass, because subsequent passes
                 * only get more restrictive.
                 */
                return null;
            }

            int index = chooseRandomInt(serverCount);
            if (index < upList.size()) {
                serviceProvider = upList.get(index);
            }
            if (serviceProvider == null) {
                /*
                 * The only time this should happen is if the serviceProvider list were
                 * somehow trimmed. This is a transient condition. Retry after
                 * yielding.
                 */
                Thread.yield();
                continue;
            }

            if (serviceProvider.isAlive()) {
                return (serviceProvider);
            }

            // Shouldn't actually happen.. but must be transient or a bug.
            serviceProvider = null;
            Thread.yield();
        }

        return serviceProvider;

    }

    protected int chooseRandomInt(int serverCount) {
        log.debug("ServiceProvider count while choosing random [{}]", serverCount);
        int value = ThreadLocalRandom.current().nextInt(serverCount);
        log.debug("Random server id chosen [{}]", value);
        return value;
    }
}
