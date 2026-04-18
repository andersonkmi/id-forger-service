package org.codecraftlabs.idgenerator.controller;

/**
 * Request body used when updating the last value of a sequence via the PUT endpoint.
 */
public class SeriesLastValue {
    private long newLastValue;

    /**
     * Sets the new last value the sequence should be moved to.
     *
     * @param newLastValue the desired last value
     */
    public void setNewLastValue(long newLastValue) {
        this.newLastValue = newLastValue;
    }

    /**
     * Returns the new last value provided in the request.
     *
     * @return the new last value
     */
    public long getNewLastValue() {
        return newLastValue;
    }
}
