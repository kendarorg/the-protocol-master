package org.kendar.server.exchange;

public class Code {
    public static final int HTTP_CONTINUE = 100;
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;

    Code() {
    }

    public static String msg(int code) {
        return switch (code) {
            case 100 -> " Continue";
            case 200 -> " OK";
            case 201 -> " Created";
            case 202 -> " Accepted";
            case 203 -> " Non-Authoritative Information";
            case 204 -> " No Content";
            case 205 -> " Reset Content";
            case 206 -> " Partial Content";
            case 300 -> " Multiple Choices";
            case 301 -> " Moved Permanently";
            case 302 -> " Temporary Redirect";
            case 303 -> " See Other";
            case 304 -> " Not Modified";
            case 305 -> " Use Proxy";
            case 400 -> " Bad Request";
            case 401 -> " Unauthorized";
            case 402 -> " Payment Required";
            case 403 -> " Forbidden";
            case 404 -> " Not Found";
            case 405 -> " Method Not Allowed";
            case 406 -> " Not Acceptable";
            case 407 -> " Proxy Authentication Required";
            case 408 -> " Request Time-Out";
            case 409 -> " Conflict";
            case 410 -> " Gone";
            case 411 -> " Length Required";
            case 412 -> " Precondition Failed";
            case 413 -> " Request Entity Too Large";
            case 414 -> " Request-URI Too Large";
            case 415 -> " Unsupported Media Type";
            case 500 -> " Internal Server Error";
            case 501 -> " Not Implemented";
            case 502 -> " Bad Gateway";
            case 503 -> " Service Unavailable";
            case 504 -> " Gateway Timeout";
            case 505 -> " HTTP Version Not Supported";
            default -> " ";
        };
    }
}
