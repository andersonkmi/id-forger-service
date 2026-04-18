package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.Sequence;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.IdService;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.codecraftlabs.idgenerator.id.service.SequenceDetailsRetrievalException;
import org.codecraftlabs.idgenerator.id.service.SequenceLastValueUpdateFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

/**
 * REST controller that exposes ID generation and sequence management endpoints
 * under {@code /idgenerator/v1/ids}.
 */
@RestController
@RequestMapping("/idgenerator/v1")
public class IdGeneratorController {
    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorController.class);
    private final IdService idService;

    /**
     * @param idService service used to generate and manage IDs
     */
    @Autowired
    public IdGeneratorController(@Nonnull IdService idService) {
        this.idService = idService;
    }

    /**
     * Generates and returns the next ID for the given series.
     *
     * @param seriesName the name of the ID series (e.g. {@code default}, {@code product})
     * @param format     optional output format ({@code plain}, {@code base64}, {@code prefixed},
     *                   {@code luhn}, {@code sha256}, {@code timestamped}); defaults to {@code plain}
     * @return HTTP 200 with the generated ID, 404 if the series is unknown,
     *         or 400 if the format is invalid or generation fails
     */
    @GetMapping(value = "/ids/{seriesName}",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> getNextId(@PathVariable String seriesName,
                                                @RequestParam(value = "format", required = false) String format) {
        try {
            String id = idService.generateId(seriesName, format);
            return generateResponse(id);
        } catch (IdNotGeneratedException exception) {
            logger.error("Id not generated", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Id not generated", exception);
        } catch (InvalidSeriesException exception) {
            logger.error("Series name is invalid", exception);
            throw new ResponseStatusException(NOT_FOUND, "Series name is invalid", exception);
        } catch (InvalidFormatException exception) {
            logger.error("Format is invalid", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Format is invalid", exception);
        }
    }

    /**
     * Updates the last value of a sequence, effectively resetting the next generated ID.
     *
     * @param seriesName      the name of the series whose sequence should be updated
     * @param seriesLastValue request body containing the new last value
     * @return HTTP 200 with the new last value, or 400 if the update fails
     */
    @PutMapping(value = "/ids/{seriesName}", produces = APPLICATION_JSON_VALUE,
            consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> changeLastValue(@PathVariable String seriesName,
                                                      @RequestBody SeriesLastValue seriesLastValue) {
        try {
            this.idService.updateSequenceLastValue(seriesName, seriesLastValue.getNewLastValue());
            return status(OK).body(new IdResponse(String.valueOf(seriesLastValue.getNewLastValue())));
        } catch (SequenceLastValueUpdateFailedException exception) {
            logger.error("Could not update the last value", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Could not update the last value", exception);
        }
    }

    /**
     * Returns the current (last generated) value for the given series without advancing the sequence.
     *
     * @param seriesName the name of the series
     * @return HTTP 200 with the current value, 404 if the series is unknown, or 400 on error
     */
    @GetMapping(value = "/ids/{seriesName}/currentValue",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<IdResponse> getCurrentId(@PathVariable String seriesName) {
        try {
            String currentValue = this.idService.getCurrentValue(seriesName);
            return generateResponse(currentValue);
        } catch (IdNotGeneratedException exception) {
            logger.error("Could not retrieve current value", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Could not retrieve current value", exception);
        } catch (InvalidSeriesException exception) {
            logger.error("Series name is invalid", exception);
            throw new ResponseStatusException(NOT_FOUND, "Series name is invalid", exception);
        }
    }

    /**
     * Retrieves full metadata for the sequence backing the given series.
     *
     * @param seriesName the name of the series
     * @return HTTP 200 with {@link Sequence} details, 404 if the series is unknown, or 400 on error
     */
    @GetMapping(value = "/ids/{seriesName}/details",
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Sequence> getSequenceDetails(@PathVariable String seriesName) {
        try {
            Sequence sequenceDetails = this.idService.getSequenceDetails(seriesName);
            return status(OK).body(sequenceDetails);
        } catch (SequenceDetailsRetrievalException exception) {
            logger.error("Could not retrieve sequence details", exception);
            throw new ResponseStatusException(BAD_REQUEST, "Could not retrieve sequence details", exception);
        } catch (InvalidSeriesException exception) {
            logger.error("Series name is invalid", exception);
            throw new ResponseStatusException(NOT_FOUND, "Series name is invalid", exception);
        }
    }

    /**
     * Handles bean-validation failures and returns a map of field names to
     * their corresponding error messages with HTTP 400.
     *
     * @param ex the validation exception thrown by Spring MVC
     * @return map of {@code fieldName -> errorMessage} pairs
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    @Nonnull
    private ResponseEntity<IdResponse> generateResponse(@Nonnull String id) {
        return status(OK).body(new IdResponse(id));
    }
}
