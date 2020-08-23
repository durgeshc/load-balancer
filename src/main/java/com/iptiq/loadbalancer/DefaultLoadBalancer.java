package com.iptiq.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultLoadBalancer implements ILoadBalancer {

    private static Logger logger = LoggerFactory.getLogger(DefaultLoadBalancer.class);

    private static final String DEFAULT_NAME = "default";
    private final static IPing DEFAULT_PING = new DummyPing();
    private final static SerialPingStrategy DEFAULT_PING_STRATEGY = new SerialPingStrategy();

    protected String name;
    protected IPing ping;
    protected IPingStrategy pingStrategy;

    protected ILoadBalancingAlgorithm rule;
    @Override
    public void setLoadBalancingAlgorithm(ILoadBalancingAlgorithm rule) {
        this.rule = rule;
    }

    protected volatile List<ServiceProvider> allServiceProviderList = Collections
            .synchronizedList(new ArrayList<ServiceProvider>());
    protected volatile List<ServiceProvider> upServiceProviderList = Collections
            .synchronizedList(new ArrayList<ServiceProvider>());

    protected ReadWriteLock allServerLock = new ReentrantReadWriteLock();
    protected ReadWriteLock upServerLock = new ReentrantReadWriteLock();

    protected Timer lbTimer = null;
    protected int pingIntervalMills = 1; // set X millis to ping
    protected AtomicBoolean pingInProgress = new AtomicBoolean(false);


    /**
     * Default constructor which sets name as "default", sets dummy ping, and
     * {@link RoundRobinLoadBalancing} as the rule.
     **/
    public DefaultLoadBalancer() {
        name = DEFAULT_NAME;
        ping = DEFAULT_PING;
        pingStrategy = DEFAULT_PING_STRATEGY;
        setLoadBalancingAlgorithm(new RoundRobinLoadBalancing(this));
        setupPingTask();
    }


    /**
     * Choose a server from load balancer.
     * @return server chosen
     */
    /*
     * Get the alive server dedicated to key
     *
     * @return the dedicated server
     */
    @Override
    public ServiceProvider chooseServer() {
        if (rule == null) {
            return null;
        } else {
            try {
                return rule.choose();
            } catch (Exception e) {
                logger.warn("LoadBalancer [{}]:  Error choosing server", name, e);
                return null;
            }
        }
    }

    /**
     * To be called by the clients of the load balancer to notify that a ServiceProvider is down
     * else, the LB will think its still Alive until the next Ping cycle - potentially
     * (assuming that the LB Impl does a ping)
     *
     * @param serviceProvider ServiceProvider to mark as down
     */
    @Override
    public void markServerDown(ServiceProvider serviceProvider) {
        if (serviceProvider == null || !serviceProvider.isAlive()) {
            return;
        }

        logger.error("LoadBalancer [{}]:  markServerDown called on [{}]", name, serviceProvider.getId());
        serviceProvider.setAlive(false);
    }

    /**
     * TimerTask that keeps runs every X seconds to check the status of each
     * server/node in the ServiceProvider List
     *
     * @author stonse
     *
     */
    class PingTask extends TimerTask {
        public void run() {
            try {
                new Pinger(pingStrategy).runPinger();
            } catch (Exception e) {
                logger.error("LoadBalancer [{}]: Error pinging", name, e);
            }
        }
    }


    /**
     * Class that contains the mechanism to "ping" all the instances
     *
     * @author stonse
     *
     */
    class Pinger {

        private final IPingStrategy pingerStrategy;

        public Pinger(IPingStrategy pingerStrategy) {
            this.pingerStrategy = pingerStrategy;
        }

        public void runPinger() throws Exception {
            if (!pingInProgress.compareAndSet(false, true)) {
                return; // Ping in progress - nothing to do
            }

            // we are "in" - we get to Ping

            ServiceProvider[] allServiceProviders = null;
            boolean[] results = null;

            Lock allLock = null;
            Lock upLock = null;

            try {
                /*
                 * The readLock should be free unless an addServer operation is
                 * going on...
                 */
                allLock = allServerLock.readLock();
                allLock.lock();
                allServiceProviders = allServiceProviderList.toArray(new ServiceProvider[allServiceProviderList.size()]);
                allLock.unlock();

                int numCandidates = allServiceProviders.length;
                results = pingerStrategy.pingServers(ping, allServiceProviders);

                final List<ServiceProvider> newUpList = new ArrayList<ServiceProvider>();
                final List<ServiceProvider> changedServiceProviders = new ArrayList<ServiceProvider>();

                for (int i = 0; i < numCandidates; i++) {
                    boolean isAlive = results[i];
                    ServiceProvider svr = allServiceProviders[i];
                    boolean oldIsAlive = svr.isAlive();

                    svr.setAlive(isAlive);

                    if (oldIsAlive != isAlive) {
                        changedServiceProviders.add(svr);
                        logger.debug("LoadBalancer [{}]:  ServiceProvider [{}] status changed to {}",
                                name, svr.getId(), (isAlive ? "ALIVE" : "DEAD"));
                    }

                    if (isAlive) {
                        newUpList.add(svr);
                    }
                }
                upLock = upServerLock.writeLock();
                upLock.lock();
                upServiceProviderList = newUpList;
                upLock.unlock();
            } finally {
                pingInProgress.set(false);
            }
        }
    }

    void setupPingTask() {
        if (lbTimer != null) {
            lbTimer.cancel();
        }
        lbTimer = new ShutdownEnabledTimer("LoadBalancer-PingTimer-" + name,
                true);
        lbTimer.schedule(new PingTask(), 0, pingIntervalMills);
        forceQuickPing();
    }

    /*
     * Force an immediate ping, if we're not currently pinging and don't have a
     * quick-ping already scheduled.
     */
    public void forceQuickPing() {
        logger.debug("LoadBalancer [{}]:  forceQuickPing invoking", name);
        try {
            new Pinger(pingStrategy).runPinger();
        } catch (Exception e) {
            logger.error("LoadBalancer [{}]: Error running forceQuickPing()", name, e);
        }
    }

    /**
     * Add a server to the 'allServer' list; does not verify uniqueness, so you
     * could give a server a greater share by adding it more than once.
     */
    public void addServer(ServiceProvider newServiceProvider) {
        if (allServiceProviderList.size() > 9) {
            /* Business exception */
            logger.error("LoadBalancer [{}]: Exception while adding ServiceProviders : [{}]", name, "More than 10 ServiceProviders not allowed");
            return;
        }
        if (newServiceProvider != null) {
            try {
                ArrayList<ServiceProvider> newList = new ArrayList<ServiceProvider>();
                newList.addAll(allServiceProviderList);
                newList.add(newServiceProvider);
                setServersList(newList);
            } catch (Exception e) {
                logger.error("LoadBalancer [{}]: Error adding newServiceProvider {}", name, newServiceProvider.getId(), e);
            }
        }
    }

    /**
     * Add a list of servers to the 'allServer' list; does not verify
     * uniqueness, so you could give a server a greater share by adding it more
     * than once
     */
    @Override
    public void addServers(List<ServiceProvider> newServiceProviders) {
        if (allServiceProviderList.size() + newServiceProviders.size() > 10) {
            /* Business exception */
            logger.error("LoadBalancer [{}]: Exception while adding ServiceProviders : [{}]", name, "More than 10 ServiceProviders not allowed");
            return;
        }
        if (newServiceProviders != null && newServiceProviders.size() > 0) {
            try {
                ArrayList<ServiceProvider> newList = new ArrayList<ServiceProvider>();
                newList.addAll(allServiceProviderList);
                newList.addAll(newServiceProviders);
                setServersList(newList);
            } catch (Exception e) {
                logger.error("LoadBalancer [{}]: Exception while adding ServiceProviders", name, e);
            }
        }
    }

    /**
     * Set the list of servers used as the server pool. This overrides existing
     * server list.
     */
    public void setServersList(List lsrv) {
        Lock writeLock = allServerLock.writeLock();
        logger.debug("LoadBalancer [{}]: clearing ServiceProvider list (SET op)", name);

        ArrayList<ServiceProvider> newServiceProviders = new ArrayList<ServiceProvider>();
        writeLock.lock();
        try {
            ArrayList<ServiceProvider> allServiceProviders = new ArrayList<ServiceProvider>();
            for (Object server : lsrv) {
                if (server == null) {
                    continue;
                }

                if (server instanceof ServiceProvider) {
                    logger.debug("LoadBalancer [{}]:  addServiceProvider [{}]", name, ((ServiceProvider) server).getId());
                    allServiceProviders.add((ServiceProvider) server);
                } else {
                    throw new IllegalArgumentException(
                            "Type String or ServiceProvider expected, instead found:"
                                    + server.getClass());
                }

            }
            boolean listChanged = false;
            if (!allServiceProviderList.equals(allServiceProviders)) {
                listChanged = true;
            }
            // This will reset readyToServe flag to true on all servers
            // regardless whether
            // previous priming connections are success or not
            allServiceProviderList = allServiceProviders;
            if (listChanged) {
                forceQuickPing();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<ServiceProvider> getReachableServers() {
        return Collections.unmodifiableList(upServiceProviderList);
    }

    @Override
    public List<ServiceProvider> getAllServers() {
        return Collections.unmodifiableList(allServiceProviderList);
    }

    @Override
    public <U, T> U get(T req) throws Exception {
        ServiceProvider s = chooseServer();
        if ( null != s ) {
            return (U) s.get(req);
        }
        else {
            throw new Exception("No downstream ServiceProviders are up");
        }
    }
}
