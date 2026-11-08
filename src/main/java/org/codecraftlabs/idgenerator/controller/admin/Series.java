package org.codecraftlabs.idgenerator.controller.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Series {
    @NotBlank
    @Size(max = 50, message = "name must be at most 10 characters long")
    private String name;
    @NotBlank
    @Size(max = 10, message = "prefix must be at most 10 characters long")
    private String prefix;

    private Long startingValue;
    private Long minimumValue;
    private Long maximumValue;
    private Long incrementBy;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setStartingValue(Long startingValue) {
        this.startingValue = startingValue;
    }

    public Long getStartingValue() {
        return this.startingValue;
    }

    public void setMinimumValue(Long minimumValue) {
        this.minimumValue = minimumValue;
    }

    public Long getMinimumValue() {
        return this.minimumValue;
    }

    public void setMaximumValue(Long maximumValue) {
        this.maximumValue = maximumValue;
    }

    public Long getMaximumValue() {
        return maximumValue;
    }

    public void setIncrementBy(Long incrementBy) {
        this.incrementBy = incrementBy;
    }

    public Long getIncrementBy() {
        return this.incrementBy;
    }
}
