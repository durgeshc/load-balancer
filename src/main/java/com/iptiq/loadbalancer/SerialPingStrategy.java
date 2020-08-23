package com.iptiq.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialPingStrategy implements IPingStrategy{
    private static Logger logger = LoggerFactory.getLogger(SerialPingStrategy.class);
    @Override
    public boolean[] pingServers(IPing ping, ServiceProvider[] serviceProviders) {
        int numCandidates = serviceProviders.length;
        boolean[] results = new boolean[numCandidates];

        logger.debug("LoadBalancer:  PingTask executing [{}] serviceProviders configured", numCandidates);

        for (int i = 0; i < numCandidates; i++) {
            results[i] = false; /* Default answer is DEAD. */
            try {
                // Real Ping can be avoided by using an in memory health data of the serviceProviders.
                if (ping != null) {
                    results[i] = ping.isAlive(serviceProviders[i]);
                }
            } catch (Exception e) {
                logger.error("Exception while pinging ServiceProvider: '{}'", serviceProviders[i], e);
            }
        }
        return results;
    }
}