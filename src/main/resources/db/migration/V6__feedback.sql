-- Guest feedback: one rating (+ optional comment) per guest visit.
CREATE TABLE feedback (
    id                     UUID          PRIMARY KEY,
    restaurant_id          UUID          NOT NULL REFERENCES restaurants(id) ON DELETE CASCADE,
    guest_session_id       UUID          NOT NULL REFERENCES guest_sessions(id) ON DELETE CASCADE,
    rating                 INTEGER       NOT NULL,
    comment                VARCHAR(1000),
    guest_name_snapshot    VARCHAR(100)  NOT NULL,
    table_number_snapshot  VARCHAR(20)   NOT NULL,
    created_at             TIMESTAMP     NOT NULL,
    updated_at             TIMESTAMP     NOT NULL
);

CREATE INDEX idx_feedback_restaurant_id ON feedback(restaurant_id);
-- One feedback per visit; a re-submit updates the existing row.
CREATE UNIQUE INDEX uq_feedback_guest_session ON feedback(guest_session_id);
