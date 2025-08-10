package org.codecraftlabs.idgenerator.id;

import org.codecraftlabs.idgenerator.id.processor.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.processor.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SequenceNotFoundException;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;

@Service
public class IdManager {
    private final UniqueIdRepository uniqueIdRepository;

    @Autowired
    public IdManager(UniqueIdRepository uniqueIdRepository) {
        this.uniqueIdRepository = uniqueIdRepository;
    }

    @Nonnull
    public String generateId(@Nonnull String seriesName) {
        long value = generateLongId(seriesName);
        return String.valueOf(value);
    }

    public long generateLongId(@Nonnull String seriesName) {
        try {
            return uniqueIdRepository.getNextId(seriesName);
        } catch (SequenceNotFoundException exception) {
            throw new InvalidSeriesException("Invalid series name provided", exception);
        } catch (DatabaseException exception) {
            throw new IdNotGeneratedException("Failed to generate id due to a database issue", exception);
        }
    }
}
