package com.smartqueue.common.enums;

public enum TokenStatus {
    WAITING,    // Booked, waiting in queue
    CALLED,     // Called to counter
    SERVING,    // Currently being served
    COMPLETED,  // Service done
    CANCELLED,  // Cancelled by user
    NO_SHOW,    // Did not show up when called
    EXPIRED     // Token expired (too old)
}
