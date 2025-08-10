package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.manager.IdManager;
import org.codecraftlabs.idgenerator.id.processor.IdFormatProcessor;
import org.codecraftlabs.idgenerator.id.processor.IdGenerationServiceFactory;
import org.codecraftlabs.idgenerator.id.processor.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.processor.InvalidSeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

@RestController
public class IdGeneratorController extends BaseControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorController.class);

    private final IdGenerationServiceFactory idGenerationServiceFactory;
    private final IdManager idManager;

    @Autowired
    public IdGeneratorController(@Nonnull IdGenerationServiceFactory idGenerationServiceFactory, @Nonnull IdManager idManager) {
        this.idGenerationServiceFactory = idGenerationServiceFactory;
        this.idManager = idManager;
    }

    @GetMapping(value = "/ids/{seriesName}",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> getNextId(@PathVariable String seriesName,
                                                @RequestParam(value = "format", required = false) String format) {
        try {
            String type = getIdGeneratorProcessorType(format);
            IdFormatProcessor processor = getProcessor(type);
            String id = processor.generateId(seriesName);
            logger.info("Generated id '{}' for series '{}' using '{}' format", id, seriesName, type);
            return generateResponse(id, seriesName);
        } catch (IdNotGeneratedException exception) {
            logger.error("Id not generated", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Id not generated", exception);
        } catch (InvalidSeriesException exception) {
            logger.error("Series name is invalid", exception);
            throw new ResponseStatusException(NOT_FOUND, "Series name is invalid", exception);
        }
    }

    @GetMapping(value = "/ids/{seriesName}/currentValue",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> getCurrentId(@PathVariable String seriesName) {
        try {
            String currentValue = this.idManager.getCurrentValue(seriesName);
            return generateResponse(currentValue, seriesName);
        } catch (IdNotGeneratedException exception) {
            logger.error("Id not generated", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Id not generated", exception);
        } catch (InvalidSeriesException exception) {
            logger.error("Series name is invalid", exception);
            throw new ResponseStatusException(NOT_FOUND, "Series name is invalid", exception);
        }
    }

    @Nonnull
    private String getIdGeneratorProcessorType(@CheckForNull String format) {
        return format != null && !format.isBlank() ? format : "default";
    }

    @Nonnull
    private IdFormatProcessor getProcessor(@Nonnull String type) {
        Optional<IdFormatProcessor> processor = idGenerationServiceFactory.getProcessor(type);
        if (processor.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid format requested");
        }
        return processor.get();
    }

    @Nonnull
    private ResponseEntity<IdResponse> generateResponse(@Nonnull String id, @Nonnull String seriesName) {
        return status(OK).body(new IdResponse(id, seriesName));
    }
}
