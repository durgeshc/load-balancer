This is a plain old style maven project. Simply import as maven project. Java 11 compatible.

Instructions related to source code
1) DefaultLoadBalancer.java is the implementation of ILoadBalancer. ``pingIntervalMills`` field is used to configure heartbeat interval in milliseconds. ``markServerDown`` method is used to mark a ServiceProvider as `healthy` and `unhealthy` explicitly.
2) ServiceProvider.java is the implementation of a IProvider. ``check()`` method in the same file is used to mimic 90% uptime. This can be considered as a way of representing real world servers.
3) RandomLoadBalancing.java and RoundRobinLoadBalancing.java are implementations of  ILoadBalancingAlgorithm.
4) Heartbeat is implemented as IPing interface and is decoupled from ILoadBalancer and ILoadBalancingAlgorithm interfaces.


Very Basic Test Cases 
``mvn clean compile test``
Note : Test may throw exception since check uses random to simulate uptime of service providers and there can be a case where no service provider is up as a result of randomization.


Few points to improve
1) Setter based injections for better testability.
2) A lot more of the test cases.
3) A lot more of the documentation.
4) Integration with Service Discovery.
