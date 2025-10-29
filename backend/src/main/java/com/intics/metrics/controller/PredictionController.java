package com.intics.metrics.controller;

import com.intics.metrics.model.PredictionResponse;
import com.intics.metrics.model.HourlyPredictionResponse;
import com.intics.metrics.model.DayWisePredictionResponse;
import com.intics.metrics.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prediction")
@CrossOrigin(origins = "*")
@Tag(name = "Traffic Prediction", description = "Endpoints for traffic forecasting using statistical algorithms")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @Operation(
            summary = "Get Next-Day Traffic Prediction",
            description = "Predicts total document ingestion for the next day using weighted averages (7-day, day-of-week, 14-day), trend detection, and confidence scoring",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully generated next-day prediction"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/next-day")
    public ResponseEntity<PredictionResponse> getNextDayPrediction() {
        return ResponseEntity.ok(predictionService.getPredictionForNextDay());
    }
    
    @Operation(
            summary = "Get 24-Hour Hourly Prediction",
            description = "Predicts hourly document ingestion counts for the next 24 hours based on historical patterns and day-of-week analysis",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully generated 24-hour hourly prediction"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/hourly")
    public ResponseEntity<HourlyPredictionResponse> getHourlyPrediction() {
        return ResponseEntity.ok(predictionService.getHourlyPredictionForNextDay());
    }
    
    @Operation(
            summary = "Get Day-Wise Prediction",
            description = "Generates daily traffic predictions for the specified number of days (1-30 days) with trend analysis and confidence levels",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully generated day-wise prediction"),
                    @ApiResponse(responseCode = "400", description = "Invalid days parameter"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GetMapping("/day-wise")
    public ResponseEntity<DayWisePredictionResponse> getDayWisePrediction(
            @Parameter(description = "Number of days to predict (min: 1, max: 30)", example = "7")
            @RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 7;
        if (days > 30) days = 30;
        return ResponseEntity.ok(predictionService.getDayWisePrediction(days));
    }
}
