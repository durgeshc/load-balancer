package com.iptiq.loadbalancer;

public interface IProvider {
    <T> String get(T req);
    boolean check();

}
