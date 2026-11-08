package org.codecraftlabs.idgenerator.controller.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request and response body used by the admin endpoint that creates a new series.
 *
 * <p>A series describes an identifier sequence: its name, the prefix applied to generated
 * identifiers, and the numeric parameters (starting, minimum and maximum values, and increment)
 * of the underlying sequence. Fields are validated with Jakarta Bean Validation constraints
 * when the request is annotated with {@code @Valid}.</p>
 */
public class Series {
    @NotBlank
    @Size(max = 50, message = "name must be at most 10 characters long")
    private String name;

    @NotBlank
    @Size(max = 10, message = "prefix must be at most 10 characters long")
    private String prefix;

    @PositiveOrZero(message = "startingValue must not be negative")
    private Long startingValue;

    @PositiveOrZero(message = "minimumValue must not be negative")
    private Long minimumValue;

    @PositiveOrZero(message = "maximumValue must not be negative")
    private Long maximumValue;

    @Positive(message = "incrementBy must be positive")
    private Long incrementBy;

    /**
     * Sets the series name.
     *
     * @param name the series name; must not be blank and at most 50 characters long
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the series name.
     *
     * @return the series name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the prefix applied to identifiers generated for this series.
     *
     * @param prefix the identifier prefix; must not be blank and at most 10 characters long
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Returns the prefix applied to identifiers generated for this series.
     *
     * @return the identifier prefix
     */
    public String getPrefix() {
        return this.prefix;
    }

    /**
     * Sets the value the underlying sequence starts from.
     *
     * @param startingValue the starting value; must be zero or positive
     */
    public void setStartingValue(Long startingValue) {
        this.startingValue = startingValue;
    }

    /**
     * Returns the value the underlying sequence starts from.
     *
     * @return the starting value
     */
    public Long getStartingValue() {
        return this.startingValue;
    }

    /**
     * Sets the minimum value allowed for the underlying sequence.
     *
     * @param minimumValue the minimum value; must be zero or positive
     */
    public void setMinimumValue(Long minimumValue) {
        this.minimumValue = minimumValue;
    }

    /**
     * Returns the minimum value allowed for the underlying sequence.
     *
     * @return the minimum value
     */
    public Long getMinimumValue() {
        return this.minimumValue;
    }

    /**
     * Sets the maximum value allowed for the underlying sequence.
     *
     * @param maximumValue the maximum value; must be zero or positive
     */
    public void setMaximumValue(Long maximumValue) {
        this.maximumValue = maximumValue;
    }

    /**
     * Returns the maximum value allowed for the underlying sequence.
     *
     * @return the maximum value
     */
    public Long getMaximumValue() {
        return maximumValue;
    }

    /**
     * Sets the amount added to the sequence on each new value.
     *
     * @param incrementBy the increment; must be positive
     */
    public void setIncrementBy(Long incrementBy) {
        this.incrementBy = incrementBy;
    }

    /**
     * Returns the amount added to the sequence on each new value.
     *
     * @return the increment
     */
    public Long getIncrementBy() {
        return this.incrementBy;
    }
}
