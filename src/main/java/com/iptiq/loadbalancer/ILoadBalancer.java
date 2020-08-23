package com.iptiq.loadbalancer;

import java.util.List;

public interface ILoadBalancer {
    /**
     * Initial list of servers.
     * This API also serves to add additional ones at a later time
     * The same logical server (host:port) could essentially be added multiple times
     * (helpful in cases where you want to give more "weightage" perhaps ..)
     *
     * @param newServiceProviders new servers to add
     */

    // Used to Register a list of providers
    void addServers(List<ServiceProvider> newServiceProviders);

    /**
     * Choose a server from load balancer.
     *
     * @return server chosen
     */
    ServiceProvider chooseServer();

    /**
     * To be called by the clients of the load balancer to notify that a ServiceProvider is down
     * else, the LB will think its still Alive until the next Ping cycle - potentially
     * (assuming that the LB Impl does a ping)
     *
     * @param serviceProvider ServiceProvider to mark as down
     */
    void markServerDown(ServiceProvider serviceProvider);


    /**
     * @return Only the servers that are up and reachable.
     */
    List<ServiceProvider> getReachableServers();

    /**
     * @return All known servers, both reachable and unreachable.
     */
    List<ServiceProvider> getAllServers();

    <U, T> U get(T req) throws Exception;

    void setLoadBalancingAlgorithm(ILoadBalancingAlgorithm algorithm);
}
