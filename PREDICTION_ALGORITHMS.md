# Traffic Prediction Algorithms Documentation

## Overview
The Inbound Ingestion Metrics Dashboard now includes a comprehensive **Traffic Prediction** module with three types of forecasting:
1. **Next-Day Prediction** - Daily total forecast for tomorrow
2. **Hourly Prediction** - 24-hour breakdown for tomorrow  
3. **Day-Wise Prediction** - 7-day forecast

---

## Algorithm 1: Next-Day Daily Total Prediction

### Purpose
Predict the total number of documents that will be ingested tomorrow.

### Data Input
- Last 30 days of daily document counts
- Document type: All types combined

### Algorithm Steps

1. **Historical Data Retrieval**
   ```sql
   SELECT DATE(pipeline_initiated_on), COUNT(*)
   FROM ingestion_downloaded_file_details
   WHERE pipeline_initiated_on >= (TODAY - 30 days)
   GROUP BY DATE(pipeline_initiated_on)
   ```

2. **Calculate Moving Averages**
   - 7-day average: Average of last 7 days
   - 14-day average: Average of last 14 days  
   - 30-day average: Average of last 30 days

3. **Day-of-Week Analysis**
   - Filter historical data for same day of week (e.g., all Mondays)
   - Calculate average for that specific day of week

4. **Weighted Prediction Formula**
   ```
   Predicted Count = (7-day avg × 0.5) + (day-of-week avg × 0.3) + (14-day avg × 0.2)
   ```
   - Recent data weighted more heavily (50% from last week)
   - Day-of-week pattern accounts for weekly seasonality (30%)
   - Medium-term trend provides stability (20%)

5. **Trend Detection**
   - Compare recent 7-day average vs previous 7-day average
   - If ratio > 1.10: **INCREASING**
   - If ratio < 0.90: **DECREASING**
   - Otherwise: **STABLE**

6. **Confidence Score Calculation**
   - Base score from data availability:
     - < 7 days: 50%
     - < 14 days: 60%
     - < 30 days: 80%
     - ≥ 30 days: Base confidence
   - Adjusted by coefficient of variation:
     - CV < 0.15: 95% confidence
     - CV < 0.30: 85% confidence
     - CV < 0.50: 70% confidence
     - CV ≥ 0.50: 60% confidence

---

## Algorithm 2: Hourly Prediction (24-Hour Breakdown)

### Purpose
Predict document traffic for each hour of tomorrow (00:00 to 23:00).

### Data Input
- Last 4 weeks of hourly data for the same day of week
- Example: If tomorrow is Tuesday, analyze last 4 Tuesdays

### Algorithm Steps

1. **Get Daily Total Prediction**
   - Use Next-Day Prediction algorithm (above) to get tomorrow's total

2. **Historical Hourly Distribution**
   ```sql
   SELECT HOUR(request_completed_on), COUNT(*)
   FROM ingestion_file_details
   WHERE DATE(request_completed_on) IN (last 4 same-day-of-weeks)
   AND DAY_OF_WEEK(request_completed_on) = tomorrow_day_of_week
   GROUP BY HOUR(request_completed_on)
   ```

3. **Calculate Distribution Percentages**
   ```
   For each hour H:
   Distribution[H] = Count[H] / Total_Count_All_Hours
   ```

4. **Apply Distribution to Predicted Total**
   ```
   For each hour H:
   Predicted_Count[H] = Daily_Predicted_Total × Distribution[H]
   ```

5. **Hourly Confidence Scoring**
   - Based on historical distribution percentage:
     - Pct > 10%: 90% confidence (peak hours)
     - Pct > 5%: 75% confidence (busy hours)
     - Pct > 2%: 60% confidence (normal hours)
     - Pct ≤ 2%: 50% confidence (low traffic hours)

6. **Weighted Recency** (Applied to historical data)
   - Week 1 (most recent): 40% weight
   - Week 2: 30% weight
   - Week 3: 20% weight
   - Week 4: 10% weight

### Example
If tomorrow's predicted total is 500 documents:
- Hour 09:00 historically represents 8% of daily traffic
- Predicted count for 09:00 = 500 × 0.08 = 40 documents

---

## Algorithm 3: Day-Wise Prediction (Next 7 Days)

### Purpose
Forecast daily document counts for the next 7 days.

### Data Input
- Last 30 days of daily document counts
- Day-of-week patterns

### Algorithm Steps

1. **Calculate Trend Multiplier**
   ```
   Recent_Avg = Average(last 7 days)
   Previous_Avg = Average(days 8-14)
   Trend_Multiplier = Recent_Avg / Previous_Avg
   ```

2. **Day-of-Week Average**
   - For each future day, calculate average for that specific weekday
   - Example: For next Monday, average all Mondays in last 4 weeks

3. **Prediction with Trend Decay**
   ```
   For day D (D = 1 to 7):
   Decay_Factor = 0.95^(D-1)  // 5% decay per day
   Predicted[D] = DayOfWeek_Avg[D] × (1 + (Trend_Multiplier - 1) × Decay_Factor)
   ```
   - Near-term predictions more influenced by current trend
   - Far-term predictions revert to historical patterns

4. **Trend Classification per Day**
   - For days 1-3:
     - Trend_Multiplier > 1.10: **INCREASING**
     - Trend_Multiplier < 0.90: **DECREASING**
     - Otherwise: **STABLE**
   - For days 4-7:
     - Trend_Multiplier > 1.10: **STABLE_UP**
     - Trend_Multiplier < 0.90: **STABLE_DOWN**
     - Otherwise: **STABLE**

5. **Confidence Decay**
   ```
   Base_Confidence = 85% (if 28+ days data) OR 70% (if < 28 days)
   For day D:
   Confidence[D] = Base_Confidence × (0.93^(D-1))  // 7% decay per day
   ```

6. **Overall Trend Detection**
   - Same as Next-Day algorithm
   - Compares recent vs previous 7-day averages

---

## Key Algorithm Features

### 1. Exponential Smoothing
- Recent data weighted more heavily than older data
- Captures recent trends while maintaining stability

### 2. Seasonal Patterns
- Day-of-week analysis accounts for weekly cycles
- Example: Mondays may have different patterns than Fridays

### 3. Confidence Decay
- Predictions further in the future have lower confidence
- Reflects increasing uncertainty over time

### 4. Adaptive Weighting
- Combines multiple time horizons (7/14/30 days)
- Balances responsiveness with stability

### 5. Statistical Rigor
- Coefficient of variation for data quality assessment
- Multiple averaging methods reduce noise

---

## API Endpoints

### 1. Next-Day Prediction
```
GET /api/prediction/next-day
```
Returns: Daily total, moving averages, trend, confidence, historical data

### 2. Hourly Prediction
```
GET /api/prediction/hourly
```
Returns: 24 hourly predictions for tomorrow with confidence scores

### 3. Day-Wise Prediction
```
GET /api/prediction/day-wise?days=7
```
Parameters:
- `days`: Number of days to predict (1-30, default: 7)

Returns: Daily predictions for next N days with trends and confidence

---

## Frontend Visualization

### Tab 3: Traffic Prediction
The dedicated Prediction tab includes:

1. **Next-Day Card** (Purple gradient)
   - Predicted count with trend chip
   - Confidence score with progress bar
   - Moving averages grid (7/14/30-day)

2. **Hourly Prediction Section**
   - Total predicted count
   - Overall confidence
   - **Bar Chart**: 24-hour breakdown showing predicted traffic by hour

3. **7-Day Forecast Section**
   - Overall trend indicator
   - Average confidence score
   - **Line Chart**: Daily predictions over next 7 days
   - **Data Table**: Detailed breakdown with date, day, count, trend, confidence

---

## Data Refresh
- Predictions auto-refresh every hour when Prediction tab is active
- Manual refresh available via "REFRESH NOW" button
- Updates incorporate latest ingestion data

---

## Use Cases

1. **Capacity Planning**: Predict peak traffic hours for resource allocation
2. **Anomaly Detection**: Compare actual vs predicted to identify issues
3. **Trend Analysis**: Identify increasing/decreasing traffic patterns
4. **Weekly Planning**: 7-day forecast for operational planning
5. **Performance Optimization**: Schedule maintenance during low-traffic hours

---

## Statistical Foundation

- **Moving Averages**: Reduce noise, identify trends
- **Weighted Combinations**: Balance multiple signals
- **Coefficient of Variation**: Measure data consistency
- **Day-of-Week Analysis**: Capture weekly seasonality
- **Exponential Decay**: Model diminishing certainty
- **Linear Regression Concepts**: Trend detection

