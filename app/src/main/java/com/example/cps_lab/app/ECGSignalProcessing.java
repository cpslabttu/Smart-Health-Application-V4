package com.example.cps_lab.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ECGSignalProcessing {

    public static ArrayList<Double> applyBandpassFilter(List<Double> ecgData, double samplingFrequency, double lowCutoff, double highCutoff) {
        // Convert cutoff frequencies to normalized form (0 to 0.5)
        double lowCutoffNormalized = lowCutoff / (samplingFrequency / 2);
        double highCutoffNormalized = highCutoff / (samplingFrequency / 2);

        // Create a Butterworth bandpass filter
        ButterworthFilter butterworthFilter = new ButterworthFilter(lowCutoffNormalized, highCutoffNormalized, samplingFrequency);

        // Apply the filter to the ECG data
        double[] filteredEcgDataArray = butterworthFilter.filter(ecgData.stream().mapToDouble(Double::doubleValue).toArray());

        // Convert filtered array back to a list
        return (ArrayList<Double>) Arrays.stream(filteredEcgDataArray).boxed().collect(Collectors.toList());
    }

    static class ButterworthFilter {
        private final double[] b;
        private final double[] a;

        public ButterworthFilter(double lowCutoff, double highCutoff, double sampleRate) {
            // Create Butterworth bandpass filter coefficients
            BandpassFilterDesign filterDesign = new BandpassFilterDesign(lowCutoff, highCutoff, sampleRate);
            this.b = filterDesign.getNumeratorCoefficients();
            this.a = filterDesign.getDenominatorCoefficients();
        }

        public double[] filter(double[] input) {
            // Apply the filter to the input signal using direct form II transposed
            double[] output = new double[input.length];
            double[] z = new double[Math.max(a.length, b.length)];
            for (int i = 0; i < input.length; i++) {
                double xi = input[i];
                double yi = b[0] * xi + z[0];
                for (int j = 1; j < b.length; j++) {
                    z[j - 1] = b[j] * xi + z[j] - a[j] * yi;
                }
                output[i] = yi;
            }
            return output;
        }
    }

    // Dummy class for BandpassFilterDesign (replace with actual implementation or use a library)
    static class BandpassFilterDesign {
        private final double[] numeratorCoefficients;
        private final double[] denominatorCoefficients;

        public BandpassFilterDesign(double lowCutoff, double highCutoff, double sampleRate) {
            // Placeholder coefficients (replace with actual filter design logic)
            this.numeratorCoefficients = new double[]{1, -2, 1};
            this.denominatorCoefficients = new double[]{1, -1.8, 0.81};
        }

        public double[] getNumeratorCoefficients() {
            return numeratorCoefficients;
        }

        public double[] getDenominatorCoefficients() {
            return denominatorCoefficients;
        }
    }

    public static boolean isNoisy(ArrayList<Double> ecgData, double samplingFrequency) {
        // Compute the mean and standard deviation of the signal
        double mean = ecgData.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = Math.sqrt(ecgData.stream().mapToDouble(val -> Math.pow(val - mean, 2)).average().orElse(0.0));

        // Thresholds for detecting noise
        double amplitudeThreshold = 2 * stdDev; // Example threshold for amplitude spikes
        double variabilityThreshold = 0.2 * stdDev; // Example threshold for variability

        // Check for amplitude spikes
        for (double value : ecgData) {
            if (Math.abs(value - mean) > amplitudeThreshold) {
                return true;
            }
        }

        // Check for excessive variability (high-frequency noise)
        for (int i = 1; i < ecgData.size(); i++) {
            if (Math.abs(ecgData.get(i) - ecgData.get(i - 1)) > variabilityThreshold) {
                return true;
            }
        }

        // Optional: Apply a low-pass filter to detect baseline wander
        // This example just uses a simple moving average for illustration
        int windowSize = (int) (samplingFrequency / 2); // Half a second window
        ArrayList<Double> smoothedData = new ArrayList<>(Collections.nCopies(ecgData.size(), 0.0));
        for (int i = 0; i < ecgData.size() - windowSize; i++) {
            double sum = 0.0;
            for (int j = 0; j < windowSize; j++) {
                sum += ecgData.get(i + j);
            }
            smoothedData.set(i + windowSize / 2, sum / windowSize);
        }

        // Check for baseline wander by comparing smoothed data to original data
        for (int i = 0; i < ecgData.size(); i++) {
            if (Math.abs(ecgData.get(i) - smoothedData.get(i)) > amplitudeThreshold) {
                return true;
            }
        }

        return false; // Signal is not considered noisy if no criteria are met
    }
}
