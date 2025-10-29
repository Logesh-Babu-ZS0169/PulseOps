package com.intics.metrics.service;

import com.intics.metrics.model.PredictionResponse;
import com.intics.metrics.model.PredictionResponse.HistoricalDataPoint;
import com.intics.metrics.model.HourlyPrediction;
import com.intics.metrics.model.HourlyPredictionResponse;
import com.intics.metrics.model.DayWisePrediction;
import com.intics.metrics.model.DayWisePredictionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public PredictionResponse getPredictionForNextDay() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        List<HistoricalDataPoint> historicalData = getHistoricalData(30);
        
        if (historicalData.isEmpty()) {
            return createEmptyPrediction(tomorrow);
        }
        
        long sevenDayAvg = calculateAverage(historicalData, 7);
        long fourteenDayAvg = calculateAverage(historicalData, 14);
        long thirtyDayAvg = calculateAverage(historicalData, 30);
        
        DayOfWeek tomorrowDayOfWeek = tomorrow.getDayOfWeek();
        long dayOfWeekAverage = calculateDayOfWeekAverage(historicalData, tomorrowDayOfWeek);
        
        long predictedCount = calculateWeightedPrediction(
            sevenDayAvg, 
            fourteenDayAvg, 
            dayOfWeekAverage
        );
        
        String trend = determineTrend(historicalData);
        
        double confidenceScore = calculateConfidenceScore(historicalData);
        
        return new PredictionResponse(
            tomorrow,
            predictedCount,
            sevenDayAvg,
            fourteenDayAvg,
            thirtyDayAvg,
            trend,
            confidenceScore,
            historicalData
        );
    }
    
    private List<HistoricalDataPoint> getHistoricalData(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        String sql = "SELECT " +
                     "    CAST(idfd.pipeline_initiated_on AS DATE) AS date, " +
                     "    COUNT(*) AS count " +
                     "FROM inbound_config.ingestion_file_details AS idf " +
                     "LEFT JOIN inbound_config.ingestion_downloaded_file_details AS idfd " +
                     "ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                     "WHERE idfd.pipeline_initiated_on >= ?::date " +
                     "AND idfd.pipeline_initiated_on < ?::date " +
                     "GROUP BY CAST(idfd.pipeline_initiated_on AS DATE) " +
                     "ORDER BY date DESC";
        
        try {
            return jdbcTemplate.query(sql, new Object[]{startDate.toString(), endDate.toString()}, 
                (rs, rowNum) -> {
                    LocalDate date = rs.getDate("date").toLocalDate();
                    return new HistoricalDataPoint(
                        date,
                        rs.getLong("count"),
                        date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    );
                });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    private long calculateAverage(List<HistoricalDataPoint> data, int days) {
        if (data.isEmpty()) return 0;
        
        List<HistoricalDataPoint> subset = data.stream()
            .limit(Math.min(days, data.size()))
            .collect(Collectors.toList());
        
        if (subset.isEmpty()) return 0;
        
        long sum = subset.stream()
            .mapToLong(HistoricalDataPoint::getCount)
            .sum();
        
        return sum / subset.size();
    }
    
    private long calculateDayOfWeekAverage(List<HistoricalDataPoint> data, DayOfWeek targetDay) {
        List<Long> sameDayCounts = data.stream()
            .filter(point -> point.getDate().getDayOfWeek() == targetDay)
            .map(HistoricalDataPoint::getCount)
            .collect(Collectors.toList());
        
        if (sameDayCounts.isEmpty()) {
            return calculateAverage(data, 7);
        }
        
        long sum = sameDayCounts.stream().mapToLong(Long::longValue).sum();
        return sum / sameDayCounts.size();
    }
    
    private long calculateWeightedPrediction(long sevenDay, long fourteenDay, long dayOfWeek) {
        return (long) ((sevenDay * 0.5) + (dayOfWeek * 0.3) + (fourteenDay * 0.2));
    }
    
    private String determineTrend(List<HistoricalDataPoint> data) {
        if (data.size() < 7) return "INSUFFICIENT_DATA";
        
        List<HistoricalDataPoint> recentWeek = data.stream().limit(7).collect(Collectors.toList());
        List<HistoricalDataPoint> previousWeek = data.stream().skip(7).limit(7).collect(Collectors.toList());
        
        if (previousWeek.isEmpty()) return "STABLE";
        
        long recentAvg = calculateAverage(recentWeek, 7);
        long previousAvg = calculateAverage(previousWeek, 7);
        
        double changePercent = ((double)(recentAvg - previousAvg) / previousAvg) * 100;
        
        if (changePercent > 10) return "INCREASING";
        if (changePercent < -10) return "DECREASING";
        return "STABLE";
    }
    
    private double calculateConfidenceScore(List<HistoricalDataPoint> data) {
        if (data.size() < 7) return 0.3;
        if (data.size() < 14) return 0.6;
        if (data.size() < 30) return 0.8;
        
        long avg = calculateAverage(data, 30);
        if (avg == 0) return 0.5;
        
        double variance = data.stream()
            .limit(30)
            .mapToDouble(point -> Math.pow(point.getCount() - avg, 2))
            .average()
            .orElse(0);
        
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / avg;
        
        if (coefficientOfVariation < 0.15) return 0.95;
        if (coefficientOfVariation < 0.30) return 0.85;
        if (coefficientOfVariation < 0.50) return 0.70;
        return 0.60;
    }
    
    private PredictionResponse createEmptyPrediction(LocalDate tomorrow) {
        return new PredictionResponse(
            tomorrow,
            0L,
            0L,
            0L,
            0L,
            "NO_DATA",
            0.0,
            new ArrayList<>()
        );
    }
    
    // ==================== HOURLY PREDICTION ====================
    
    public HourlyPredictionResponse getHourlyPredictionForNextDay() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        DayOfWeek tomorrowDayOfWeek = tomorrow.getDayOfWeek();
        
        // Get next-day total prediction first
        PredictionResponse dailyPrediction = getPredictionForNextDay();
        long totalPredicted = dailyPrediction.getPredictedCount();
        
        if (totalPredicted == 0) {
            return createEmptyHourlyPrediction(tomorrow);
        }
        
        // Get historical hourly distribution for same day of week
        Map<Integer, Double> hourlyDistribution = getHourlyDistribution(tomorrowDayOfWeek);
        
        // Generate hourly predictions
        List<HourlyPrediction> hourlyPredictions = new ArrayList<>();
        double totalConfidence = 0.0;
        
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourStart = tomorrow.atTime(hour, 0);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            
            double distributionPct = hourlyDistribution.getOrDefault(hour, 0.0);
            long predictedCount = Math.round(totalPredicted * distributionPct);
            
            double hourConfidence = calculateHourConfidence(hour, hourlyDistribution);
            totalConfidence += hourConfidence;
            
            String timeLabel = String.format("%02d:00", hour);
            
            hourlyPredictions.add(new HourlyPrediction(
                hourStart,
                hourEnd,
                predictedCount,
                hourConfidence,
                hour,
                timeLabel
            ));
        }
        
        double overallConfidence = totalConfidence / 24.0;
        
        return new HourlyPredictionResponse(
            tomorrow,
            totalPredicted,
            hourlyPredictions,
            overallConfidence,
            "Hourly prediction based on historical patterns for " + 
            tomorrowDayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        );
    }
    
    private Map<Integer, Double> getHourlyDistribution(DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        Map<Integer, Double> distribution = new HashMap<>();
        
        // Get last 4 weeks of data for the same day of week
        String sql = "SELECT " +
                     "    EXTRACT(HOUR FROM idf.request_completed_on) AS hour, " +
                     "    COUNT(*) AS count " +
                     "FROM inbound_config.ingestion_file_details AS idf " +
                     "JOIN inbound_config.ingestion_downloaded_file_details AS idfd " +
                     "ON idf.inbound_transaction_id = idfd.inbound_transaction_id " +
                     "WHERE CAST(idf.request_completed_on AS DATE) >= ?::date - interval '28 days' " +
                     "AND CAST(idf.request_completed_on AS DATE) < ?::date " +
                     "AND EXTRACT(DOW FROM idf.request_completed_on) = ? " +
                     "GROUP BY EXTRACT(HOUR FROM idf.request_completed_on) " +
                     "ORDER BY hour";
        
        try {
            int dayOfWeekNum = (dayOfWeek.getValue() % 7); // Convert to PostgreSQL DOW (0=Sunday)
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql, 
                today.toString(), 
                today.toString(),
                dayOfWeekNum
            );
            
            if (results.isEmpty()) {
                // Default uniform distribution if no data
                for (int i = 0; i < 24; i++) {
                    distribution.put(i, 1.0 / 24.0);
                }
                return distribution;
            }
            
            // Calculate total count
            long totalCount = results.stream()
                .mapToLong(row -> ((Number) row.get("count")).longValue())
                .sum();
            
            // Calculate distribution percentages
            for (Map<String, Object> row : results) {
                int hour = ((Number) row.get("hour")).intValue();
                long count = ((Number) row.get("count")).longValue();
                distribution.put(hour, (double) count / totalCount);
            }
            
            // Fill missing hours with 0
            for (int i = 0; i < 24; i++) {
                distribution.putIfAbsent(i, 0.0);
            }
            
        } catch (Exception e) {
            // Fallback to uniform distribution
            for (int i = 0; i < 24; i++) {
                distribution.put(i, 1.0 / 24.0);
            }
        }
        
        return distribution;
    }
    
    private double calculateHourConfidence(int hour, Map<Integer, Double> distribution) {
        double pct = distribution.getOrDefault(hour, 0.0);
        // Higher percentage = higher confidence (more consistent pattern)
        if (pct > 0.10) return 0.90;
        if (pct > 0.05) return 0.75;
        if (pct > 0.02) return 0.60;
        return 0.50;
    }
    
    private HourlyPredictionResponse createEmptyHourlyPrediction(LocalDate date) {
        List<HourlyPrediction> emptyHours = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            LocalDateTime hourStart = date.atTime(hour, 0);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            String timeLabel = String.format("%02d:00", hour);
            emptyHours.add(new HourlyPrediction(hourStart, hourEnd, 0L, 0.0, hour, timeLabel));
        }
        return new HourlyPredictionResponse(date, 0L, emptyHours, 0.0, "No historical data available");
    }
    
    // ==================== DAY-WISE PREDICTION ====================
    
    public DayWisePredictionResponse getDayWisePrediction(int days) {
        List<DayWisePrediction> predictions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Get historical data for pattern analysis
        List<HistoricalDataPoint> historicalData = getHistoricalData(30);
        
        if (historicalData.isEmpty()) {
            return createEmptyDayWisePrediction(days);
        }
        
        // Calculate trend multiplier
        double trendMultiplier = calculateTrendMultiplier(historicalData);
        String overallTrend = determineTrend(historicalData);
        
        double totalConfidence = 0.0;
        
        for (int i = 1; i <= days; i++) {
            LocalDate futureDate = today.plusDays(i);
            DayOfWeek dayOfWeek = futureDate.getDayOfWeek();
            
            // Get average for this specific day of week from last 4 weeks
            long dayOfWeekAvg = calculateDayOfWeekAverage(historicalData, dayOfWeek);
            
            // Apply trend multiplier with decay (further dates = less trend influence)
            double decayFactor = Math.pow(0.95, i - 1); // 5% decay per day
            long predictedCount = Math.round(dayOfWeekAvg * (1 + (trendMultiplier - 1) * decayFactor));
            
            // Ensure non-negative
            predictedCount = Math.max(0, predictedCount);
            
            // Calculate confidence (decreases with distance)
            double confidence = calculateDayPredictionConfidence(historicalData, i);
            totalConfidence += confidence;
            
            String dayTrend = predictTrendForDay(i, trendMultiplier);
            
            predictions.add(new DayWisePrediction(
                futureDate,
                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                predictedCount,
                dayTrend,
                confidence
            ));
        }
        
        double avgConfidence = totalConfidence / days;
        
        return new DayWisePredictionResponse(
            predictions,
            overallTrend,
            avgConfidence,
            "Prediction for next " + days + " days based on weekly patterns and trend analysis"
        );
    }
    
    private double calculateTrendMultiplier(List<HistoricalDataPoint> data) {
        if (data.size() < 14) return 1.0;
        
        long recentAvg = calculateAverage(data, 7);
        long previousAvg = (long) data.stream()
            .skip(7)
            .limit(7)
            .mapToLong(HistoricalDataPoint::getCount)
            .average()
            .orElse((double) recentAvg);
        
        if (previousAvg == 0) return 1.0;
        
        return (double) recentAvg / previousAvg;
    }
    
    private String predictTrendForDay(int daysAhead, double trendMultiplier) {
        if (trendMultiplier > 1.10) {
            return daysAhead <= 3 ? "INCREASING" : "STABLE_UP";
        } else if (trendMultiplier < 0.90) {
            return daysAhead <= 3 ? "DECREASING" : "STABLE_DOWN";
        }
        return "STABLE";
    }
    
    private double calculateDayPredictionConfidence(List<HistoricalDataPoint> data, int daysAhead) {
        // Base confidence from data quality
        double baseConfidence = data.size() >= 28 ? 0.85 : 0.70;
        
        // Decay confidence for further dates
        double decayFactor = Math.pow(0.93, daysAhead - 1); // 7% decay per day
        
        return baseConfidence * decayFactor;
    }
    
    private DayWisePredictionResponse createEmptyDayWisePrediction(int days) {
        List<DayWisePrediction> emptyPredictions = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 1; i <= days; i++) {
            LocalDate futureDate = today.plusDays(i);
            DayOfWeek dayOfWeek = futureDate.getDayOfWeek();
            emptyPredictions.add(new DayWisePrediction(
                futureDate,
                dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                0L,
                "NO_DATA",
                0.0
            ));
        }
        
        return new DayWisePredictionResponse(
            emptyPredictions,
            "NO_DATA",
            0.0,
            "Insufficient historical data for prediction"
        );
    }
}
