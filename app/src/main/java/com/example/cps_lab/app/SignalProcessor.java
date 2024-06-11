package com.example.cps_lab.app;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class SignalProcessor {

    public static List<Double[]> detectApnea(List<Double> signal, double fs, double thresholdFactor, double minDuration) {
        double threshold = thresholdFactor * Collections.max(signal);
        int minDurationSamples = (int) (minDuration * fs);

        // Find where the signal is below the threshold
        List<Double[]> apneaEvents = new ArrayList<>();
        signal = preprocessSignal(signal, fs);
        int startIdx = -1;
        for (int i = 0; i < signal.size(); i++) {
            if (signal.get(i) < threshold) {
                if (startIdx == -1) {
                    startIdx = i;  // Start of an apnea event
                }
            } else {
                if (startIdx != -1) {
                    int durationSamples = i - startIdx;
                    if (durationSamples >= minDurationSamples) {
                        double startTime = startIdx / fs;
                        double endTime = i / fs;
                        apneaEvents.add(new Double[]{startTime, endTime});  // Convert indices to time
                    }
                    startIdx = -1;
                }
            }
        }

        // Handle case where signal ends during an apnea event
        if (startIdx != -1) {
            int durationSamples = signal.size() - startIdx;
            if (durationSamples >= minDurationSamples) {
                double startTime = startIdx / fs;
                double endTime = signal.size() / fs;
                apneaEvents.add(new Double[]{startTime, endTime});
            }
        }

        return apneaEvents;
    }

    public static List<Double> preprocessSignal(List<Double> signal, double fs) {
        // Replace 'inf' values with NaNs
        for (int i = 0; i < signal.size(); i++) {
            if (Double.isInfinite(signal.get(i))) {
                signal.set(i, Double.NaN);
            }
        }

        // Replace NaN values with the mean of the signal, ignoring NaNs in the computation
        boolean hasNaN = false;
        double signalSum = 0.0;
        int validCount = 0;
        for (Double value : signal) {
            if (!Double.isNaN(value)) {
                signalSum += value;
                validCount++;
            } else {
                hasNaN = true;
            }
        }
        if (hasNaN) {
            double signalMean = signalSum / validCount;
            for (int i = 0; i < signal.size(); i++) {
                if (Double.isNaN(signal.get(i))) {
                    signal.set(i, signalMean);
                }
            }
        }

        // Detrend the signal to remove baseline drift
        List<Double> detrendedSignal = detrend(signal);

        // Define low-pass filter parameters
        double cutoffFreq = 0.5;  // Hz, adjust based on your signal characteristics
        double nyq = 0.5 * fs;  // Nyquist frequency
        double normalCutoff = cutoffFreq / nyq;

        // Apply the filter
        List<Double> filteredSignal = lowPassFilter(detrendedSignal, normalCutoff);

        return filteredSignal;
    }

    private static List<Double> detrend(List<Double> signal) {
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < signal.size(); i++) {
            regression.addData(i, signal.get(i));
        }

        double intercept = regression.getIntercept();
        double slope = regression.getSlope();

        List<Double> detrendedSignal = new ArrayList<>();
        for (int i = 0; i < signal.size(); i++) {
            detrendedSignal.add(signal.get(i) - (slope * i + intercept));
        }
        return detrendedSignal;
    }

    private static List<Double> lowPassFilter(List<Double> signal, double normalCutoff) {
        // Apply Butterworth low-pass filter
        List<Double> filteredSignal = new ArrayList<>(Collections.nCopies(signal.size(), 0.0));
        // Add your low-pass filter implementation here
        return filteredSignal;
    }
}
