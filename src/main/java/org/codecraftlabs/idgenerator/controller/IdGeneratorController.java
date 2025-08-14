package org.codecraftlabs.idgenerator.controller;

import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.IdService;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.status;

@RestController
public class IdGeneratorController extends BaseControllerV1 {
    private static final Logger logger = LoggerFactory.getLogger(IdGeneratorController.class);
    private final IdService idService;

    @Autowired
    public IdGeneratorController(@Nonnull IdService idService) {
        this.idService = idService;
    }

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

    @Nonnull
    private ResponseEntity<IdResponse> generateResponse(@Nonnull String id) {
        return status(OK).body(new IdResponse(id));
    }
}
