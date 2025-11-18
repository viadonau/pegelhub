
package com.stm.pegelhub.inbound.data.controller;

import com.stm.pegelhub.common.model.data.Measurement;
import com.stm.pegelhub.inbound.data.dto.WriteMeasurementsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Rest Controller for all {@code Measurement}s.
 */
@RestController
@RequestMapping("/api/v1/measurement")
public interface MeasurementController {

    @Operation(summary = "Adds a new Measurement Entry to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "The given measurement was successfully created.")
    })
    @PostMapping
    void writeMeasurementData(@RequestParam(name = "apiKey", defaultValue = "m935dV-0eTtwLiTqNNCO9ZhjyxfywmKUR7S_KwLPMcpfPPtM1wbJXHc9WXnSwiydVs3_loDF1vd_CSSyPSo73w==") String apiKey, @RequestBody WriteMeasurementsDto measurements);

    @Operation(summary = "Gets all Measurement Data in Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Measurement Data in Range",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })

    @GetMapping("/{range}")
    List<Measurement> findMeasurementInRange(@RequestParam(name = "apiKey", defaultValue = "") String apiKey, @PathVariable String range);

    @Operation(summary = "Gets all Measurement Data for Supplier In Range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns all Measurement Data for Supplier",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/supplier/{range}")
    List<Measurement> findMeasurementForSupplierInRange(@RequestParam(name = "apiKey", defaultValue = "") String apiKey,
                                                        @RequestParam(name = "stationNumber", defaultValue = "") String stationNumber,
                                                        @PathVariable String range);


    @Operation(summary = "Gets latest Measurement from Supplier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the latest measurement",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/supplier/latest")
    Measurement findLatestMeasurementBySupplier(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestParam(name = "stationNumber", defaultValue = "") String stationNumber
    );

    @Operation(summary = "Gets the average of all measurement fields from a Supplier over a given time range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns a single Measurement object where each field is the calculated average over the time range.",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))}),
            @ApiResponse(responseCode = "404", description = "No supplier found for the given station number or no data available in the specified range.")
    })
    @GetMapping("/supplier/average/{range}")
    Measurement findAverageMeasurementForSupplierInRange(
            @RequestParam(name = "apiKey", defaultValue = "") String apiKey,
            @RequestParam(name = "stationNumber") String stationNumber,
            @PathVariable String range
    );

    @Operation(summary = "Gets last Measurement entry for ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the measurement entry",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Measurement.class))})
    })
    @GetMapping("/last/{uuid}")
    Measurement findMeasurementById(@RequestParam(name = "apiKey", defaultValue = "") String apiKey, @PathVariable UUID uuid);

    @GetMapping("/systemTime")
    Timestamp getSystemtime();
}
