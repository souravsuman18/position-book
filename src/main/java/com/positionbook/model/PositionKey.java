package com.positionbook.model;

/**
 * Composite identity of a position, uniquely determined by account and security.
 *
 * @param accountId account identifier
 * @param securityId security identifier
 */
public record PositionKey(String accountId, String securityId) {
}
