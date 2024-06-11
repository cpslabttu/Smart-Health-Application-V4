package com.example.cps_lab.app;

import java.util.ArrayList;
import java.util.List;

public class ButterworthFilter {

    private static final double SQRT2 = Math.sqrt(2.0);

    public static List<Double> butterworthFilter(List<Double> signal, double cutoffFrequency, double samplingRate) {
        int n = signal.size();
        double[] filteredSignal = new double[n];

        double wc = Math.tan(Math.PI * cutoffFrequency / samplingRate);
        double k1 = SQRT2 * wc;
        double k2 = wc * wc;
        double a0, a1, a2, b1, b2;

        a0 = k2 / (1 + k1 + k2);
        a1 = 2 * a0;
        a2 = a0;
        b1 = 2 * a0 * (1 - k2) / (1 + k1 + k2);
        b2 = a0 * (1 - k1 + k2) / (1 + k1 + k2);

        double[] w = new double[n];
        double[] x = new double[n];

        for (int i = 0; i < n; i++) {
            double input = signal.get(i);
            if (i > 1) {
                w[i] = input - b1 * w[i - 1] - b2 * w[i - 2];
            } else if (i > 0) {
                w[i] = input - b1 * w[i - 1];
            } else {
                w[i] = input;
            }
            if (i > 1) {
                filteredSignal[i] = a0 * w[i] + a1 * w[i - 1] + a2 * w[i - 2];
            } else if (i > 0) {
                filteredSignal[i] = a0 * w[i] + a1 * w[i - 1];
            } else {
                filteredSignal[i] = a0 * w[i];
            }
        }

        for (int i = 0; i < n; i++) {
            double input = filteredSignal[i];
            if (i > 1) {
                x[i] = input - b1 * x[i - 1] - b2 * x[i - 2];
            } else if (i > 0) {
                x[i] = input - b1 * x[i - 1];
            } else {
                x[i] = input;
            }
            if (i > 1) {
                filteredSignal[i] = a0 * x[i] + a1 * x[i - 1] + a2 * x[i - 2];
            } else if (i > 0) {
                filteredSignal[i] = a0 * x[i] + a1 * x[i - 1];
            } else {
                filteredSignal[i] = a0 * x[i];
            }
        }

        List<Double> filtered = new ArrayList<>();
        for (double v : filteredSignal) {
            filtered.add(v);
        }
        return filtered;
    }

    public static List<Double> lowPassFilter(List<Double> signal, double cutoffFrequency, double samplingRate) {
        int n = signal.size();
        List<Double> filteredSignal = new ArrayList<>(n);

        // Filter order (4th order)
        int order = 4;

        // Pre-warp the cutoff frequency
        double wc = Math.tan(Math.PI * cutoffFrequency / samplingRate);

        // Calculate the coefficients for the bilinear transform
        double[] a = new double[order + 1];
        double[] b = new double[order + 1];
        double[] A = new double[order + 1];
        double[] B = new double[order + 1];

        double sqrt2 = Math.sqrt(2);

        b[0] = wc * wc;
        b[1] = 2 * b[0];
        b[2] = b[0];

        a[0] = 1 + sqrt2 * wc + wc * wc;
        a[1] = -2 + 2 * wc * wc;
        a[2] = 1 - sqrt2 * wc + wc * wc;

        for (int i = 0; i <= order; i++) {
            A[i] = b[i] / a[0];
            B[i] = a[i] / a[0];
        }

        // Initialize the state variables
        double[] w = new double[order + 1];

        for (int i = 0; i < n; i++) {
            double input = signal.get(i);

            for (int j = order; j >= 2; j--) {
                w[j] = w[j - 1];
            }

            w[0] = input - B[1] * w[1] - B[2] * w[2];
            double output = A[0] * w[0] + A[1] * w[1] + A[2] * w[2];

            filteredSignal.add(output);
        }

        return filteredSignal;
    }
}
