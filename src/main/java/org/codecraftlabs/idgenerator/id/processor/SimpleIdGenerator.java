package org.codecraftlabs.idgenerator.id.processor;

import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdRepository;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
class SimpleIdGenerator {
    private final UniqueIdRepository uniqueIdRepository;

    @Autowired
    SimpleIdGenerator(UniqueIdRepository uniqueIdRepository) {
        this.uniqueIdRepository = uniqueIdRepository;
    }

    @Nonnull
    String generateId(@Nonnull String seriesName) {
        long value = generateLongId(seriesName);
        return String.valueOf(value);
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
}
