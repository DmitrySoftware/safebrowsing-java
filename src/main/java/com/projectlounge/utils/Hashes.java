package com.projectlounge.utils;

/**
 * Created by main on 23.08.17.
 */
public interface Hashes {

    boolean matches(byte[] hash);

    Hashes combine(byte[] hash);
    Hashes remove(byte[] hash);



}
