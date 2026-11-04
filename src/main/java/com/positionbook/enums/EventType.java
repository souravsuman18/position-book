package com.positionbook.enums;

/** Supported trade event types. */
public enum EventType {
    /** Adds the event quantity to the position. */
    BUY,
    /** Subtracts the event quantity from the position when sufficient quantity exists. */
    SELL,
    /** Reverses a previously accepted BUY or SELL event. */
    CANCEL
}