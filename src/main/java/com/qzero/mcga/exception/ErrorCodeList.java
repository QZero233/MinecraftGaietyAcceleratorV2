package com.qzero.mcga.exception;

public class ErrorCodeList {

    /**
     * Indicates that login info such as username and password is wrong
     * Login failed
     */
    public static final int WRONG_LOGIN_INFO=-100;

    /**
     * Some necessary request parameter is missing
     */
    public static final int MISSING_REQUEST_PARAMETER=-101;

    /**
     * Caused by unknown error
     */
    public static final int UNKNOWN_ERROR=-102;

    /**
     * Indicates that the token is wrong
     */
    public static final int INVALIDATE_TOKEN=-103;

    /**
     * Permission denied
     */
    public static final int PERMISSION_DENIED=-110;


}
