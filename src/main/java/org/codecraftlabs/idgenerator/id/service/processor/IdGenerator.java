package org.codecraftlabs.idgenerator.id.service.processor;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdRepository;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;

import static java.lang.String.valueOf;

@Service
public class IdGenerator {
    private final UniqueIdRepository uniqueIdRepository;

    @Autowired
    public IdGenerator(UniqueIdRepository uniqueIdRepository) {
        this.uniqueIdRepository = uniqueIdRepository;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        long value = generateLongId(seriesName);
        return valueOf(value);
    }

    @Nonnull
    public String getCurrentValue(@Nonnull String seriesName) {
        try {
            return valueOf(uniqueIdRepository.getCurrentId(seriesName));
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }

    long generateLongId(@Nonnull String seriesName) {
        try {
            return uniqueIdRepository.getNextId(seriesName);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }

    @Nonnull
    public Sequence getSequenceDetails(@Nonnull String name) {
        try {
            return this.uniqueIdRepository.getSequenceDetails("public", name);
        } catch (DatabaseException exception) {
            throw new SequenceDetailsRetrievalException("Failed to get details about the sequence", exception);
        }
    }

    public void updateSequenceLastValue(@Nonnull String name, long lastValue) {
        try {
            this.uniqueIdRepository.updateSequenceLastValue(name, lastValue);
        } catch (DatabaseException exception) {
            throw new SequenceLastValueUpdateFailedException("Failed to update sequence last value", exception);
        }
    }
}
