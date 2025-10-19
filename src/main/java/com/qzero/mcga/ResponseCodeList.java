package com.qzero.mcga;

public class ResponseCodeList {

    /**
     * Indicates that everything is ok
     * Action succeeded
     */
    public static final int OK=200;

    /**
     * Some known error such as wrong username, happened
     */
    public static final int KNOWN_ERROR=-1;

    /**
     * Some unknown error happened
     */
    public static final int UNKNOWN_ERROR=-2;

}
