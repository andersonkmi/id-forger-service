package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.IdService;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.Nonnull;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

/**
 * REST controller dedicated to the ID generation hot path.
 * Exposes the high-volume, latency-sensitive endpoint used by clients
 * to retrieve newly generated identifiers under {@code /idgenerator/v1/ids}.
 */
@RestController
@RequestMapping("/idgenerator/v1")
public class IdGenerationController {
    private static final Logger logger = LoggerFactory.getLogger(IdGenerationController.class);
    private final IdService idService;

    /**
     * @param idService service used to generate IDs
     */
    public IdGenerationController(@Nonnull IdService idService) {
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
            return status(OK).body(new IdResponse(id));
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
}
