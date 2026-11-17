package com.pccheck.steam;

public class SteamUnavailableException extends RuntimeException {
    public SteamUnavailableException(String message) { super(message); }
    public SteamUnavailableException(String message, Throwable cause) { super(message, cause); }
}
