package com.iptiq.loadbalancer;


/**
 * Interface that defines how we "ping" a server to check if its alive
 *
 */

public interface IPing {

    /**
     * Checks whether the given <code>ServiceProvider</code> is "alive" i.e. should be
     * considered a candidate while loadbalancing
     *
     */
    boolean isAlive(ServiceProvider serviceProvider);

}