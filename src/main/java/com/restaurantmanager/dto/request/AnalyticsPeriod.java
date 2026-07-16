package com.restaurantmanager.dto.request;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * The reporting window for the sales summary. Each value knows how to compute
 * its own start instant relative to "now" (rolling windows, UTC-day aligned for
 * DAY to match how orders are timestamped).
 */
public enum AnalyticsPeriod {

    DAY {
        @Override
        public Instant from(Instant now) {
            return now.truncatedTo(ChronoUnit.DAYS);
        }
    },
    WEEK {
        @Override
        public Instant from(Instant now) {
            return now.minus(7, ChronoUnit.DAYS);
        }
    },
    MONTH {
        @Override
        public Instant from(Instant now) {
            return now.minus(30, ChronoUnit.DAYS);
        }
    };

    public abstract Instant from(Instant now);
}
