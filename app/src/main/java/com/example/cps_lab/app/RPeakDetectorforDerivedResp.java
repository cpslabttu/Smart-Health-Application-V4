package com.example.cps_lab.app;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.DstNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

public class RPeakDetectorforDerivedResp {
    private static final int WINDOW_SIZE = 30; // Window size in samples

    public static List<Integer> detectRPeaks(List<Double> ecgData) {

        List<Double> notchFilteredData = notchFilter(ecgData);

        // Apply bandpass filter
        List<Double> filteredData = bandpassFilter(notchFilteredData);

        List<Double> highPassFilteredData = highpassFilter(filteredData);

        // Apply differentiation
        List<Double> diffData = differentiate(highPassFilteredData);

        // Apply squaring
        List<Double> squaredData = square(diffData);

        // Apply moving window integration
        List<Double> integratedData = movingWindowIntegration(squaredData);

        // Detect peaks using thresholding
        return findPeaks(integratedData);
    }

    public static List<Double> detectRPeaksAmplitude(List<Double> ecgData) {
        List<Integer> rPeaks = detectRPeaks(ecgData);
        List<Double> amplitudes = new ArrayList<>();
        double mean = 0;
        for(int peak : rPeaks){
            mean += ecgData.get(peak);
        }
        mean /= rPeaks.size();


        for (int peak : rPeaks) {
            if(ecgData.get(peak) > mean){
                amplitudes.add(mean * 0.9);
            }
            else {
                amplitudes.add(ecgData.get(peak));
            }
        }

        return amplitudes;
    }

    private static List<Double> notchFilter(List<Double> data) {
        double[] notchB = {0.96508083, -1.84794186, 0.96508083};
        double[] notchA = {1.0, -1.84794186, 0.93016166};
        return applyFilter(data, notchB, notchA);
    }

    private static List<Double> applyFilter(List<Double> data, double[] b, double[] a) {
        double[] input = data.stream().mapToDouble(Double::doubleValue).toArray();
        double[] output = new double[input.length];

        output[0] = b[0] * input[0];
        for (int i = 1; i < input.length; i++) {
            output[i] = b[0] * input[i];
            for (int j = 1; j < b.length; j++) {
                if (i - j >= 0) {
                    output[i] += b[j] * input[i - j];
                }
            }
            for (int j = 1; j < a.length; j++) {
                if (i - j >= 0) {
                    output[i] -= a[j] * output[i - j];
                }
            }
        }

        List<Double> filteredData = new ArrayList<>();
        for (double v : output) {
            filteredData.add(v);
        }
        return filteredData;
    }

    private static List<Double> bandpassFilter(List<Double> data) {
        return ButterworthFilter.butterworthFilter(data, 150, 1000);
    }

    private static List<Double> highpassFilter(List<Double> data) {
        // Design and apply a high-pass Butterworth filter
        int order = 2; // Order of the Butterworth filter
        double[] coefficients = designHighpassButterworth(order, 1000, 0.5);
        return applyHighpassFilter(data, coefficients);
    }

    private static double[] designHighpassButterworth(int order, double sampleRate, double cutoffFrequency) {
        double nyquist = 0.5 * sampleRate;
        double normalizedCutoff = cutoffFrequency / nyquist;

        // Pre-warping
        double warpedCutoff = Math.tan(Math.PI * normalizedCutoff) / (Math.PI * normalizedCutoff);

        // Compute poles and zeros
        Complex[] poles = new Complex[order];
        for (int k = 0; k < order; k++) {
            double theta = Math.PI * (2.0 * k + 1.0) / (2.0 * order);
            poles[k] = new Complex(-Math.sin(theta), Math.cos(theta));
        }

        // Bilinear transform
        Complex[] transformedPoles = new Complex[order];
        for (int k = 0; k < order; k++) {
            transformedPoles[k] = poles[k].add(warpedCutoff).divide(poles[k].subtract(warpedCutoff)).multiply(-1);
        }

        // Compute filter coefficients
        double[] a = new double[order + 1];
        double[] b = new double[order + 1];
        a[0] = 1.0;
        b[0] = 1.0;
        for (int k = 0; k < order; k++) {
            a[1] += -transformedPoles[k].getReal();
            b[1] += transformedPoles[k].getReal();
        }
        for (int k = 0; k < order; k++) {
            for (int j = k + 1; j > 0; j--) {
                a[j] += -transformedPoles[k].getReal() * a[j - 1];
                b[j] += transformedPoles[k].getReal() * b[j - 1];
            }
        }

        return new double[]{a[0], a[1], b[0], b[1]};
    }

    private static List<Double> applyHighpassFilter(List<Double> data, double[] coefficients) {
        double[] input = data.stream().mapToDouble(Double::doubleValue).toArray();
        double[] output = new double[input.length];

        double a0 = coefficients[0];
        double a1 = coefficients[1];
        double b0 = coefficients[2];
        double b1 = coefficients[3];

        output[0] = b0 * input[0];
        for (int i = 1; i < input.length; i++) {
            output[i] = b0 * input[i] + b1 * input[i - 1] - a1 * output[i - 1];
        }

        List<Double> filteredData = new ArrayList<>();
        for (double v : output) {
            filteredData.add(v);
        }
        return filteredData;
    }

    private static List<Double> differentiate(List<Double> data) {
        List<Double> diffData = new ArrayList<>();

        for (int i = 1; i < data.size(); i++) {
            diffData.add(data.get(i) - data.get(i - 1));
        }

        return diffData;
    }

    private static List<Double> square(List<Double> data) {
        List<Double> squaredData = new ArrayList<>();

        for (double value : data) {
            squaredData.add(value * value);
        }

        return squaredData;
    }

    private static List<Double> movingWindowIntegration(List<Double> data) {
        List<Double> integratedData = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < WINDOW_SIZE; j++) {
                if (i - j >= 0) {
                    sum += data.get(i - j);
                }
            }
            integratedData.add(sum / WINDOW_SIZE);
        }

        return integratedData;
    }

    private static List<Integer> findPeaks(List<Double> data) {
        List<Integer> peaks = new ArrayList<>();
        double threshold = findThreshold(data);

        for (int i = 1; i < data.size() - 1; i++) {
            if (data.get(i) > threshold && data.get(i) > data.get(i - 1) && data.get(i) > data.get(i + 1)) {
                peaks.add(i);
                i += 200;
            }
        }

        return peaks;
    }

    public static double findThreshold(List<Double> differentiatedSignal) {
        double mean = 0;
        double std = 0;
        for (double val : differentiatedSignal) {
            mean += val;
        }
        mean /= differentiatedSignal.size();

        for (double val : differentiatedSignal) {
            std += Math.pow(val - mean, 2);
        }
        std = Math.sqrt(std / differentiatedSignal.size());
        return mean + std * 2;
    }


}