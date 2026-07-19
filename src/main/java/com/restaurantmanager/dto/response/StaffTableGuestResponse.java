package com.restaurantmanager.dto.response;

import java.util.UUID;

/** One active guest session at a table, as seen on the staff floor view. */
public record StaffTableGuestResponse(
        UUID sessionId,
        String guestName,
        boolean billRequested,
        /** Null when the guest didn't give a phone number. */
        Integer visitCount
) {
}
