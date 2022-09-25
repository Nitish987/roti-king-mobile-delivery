package com.rotiking.delivery.utils;

public interface Promise<T> {
    void resolving(int progress, String msg);
    void resolved(T o);
    void reject(String err);
}
