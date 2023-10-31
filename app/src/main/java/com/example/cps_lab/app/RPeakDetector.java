package com.example.cps_lab.app;

import java.util.ArrayList;
import java.util.List;

public class RPeakDetector {


    public static ArrayList<Integer> detectRPeaks(List<Double> data){
        double[] ecg = new double[data.size()];
        for(int i=0;i<data.size();i++){
            ecg[i] = data.get(i);
        }

        double[] filteredEcg = filterEcg(ecg);

        // Square the filtered ECG signal
        double[] squaredEcg = squareEcg(filteredEcg);

        // Integrate the squared ECG signal
        double[] integratedEcg = integrateEcg(squaredEcg);

        // Differentiate the integrated ECG signal
        double[] differentiatedEcg = differentiateEcg(integratedEcg);
//        for (int i=0;i<differentiatedEcg.length;i++){
//            System.out.println("DIFF " + differentiatedEcg[i]);
//        }

        double[] thresholdedSignal =  applyThreshold(differentiatedEcg);
        ArrayList<Integer> rPeaks = new ArrayList<>();

        for (int i = 0; i < thresholdedSignal.length; i++) {
            if (thresholdedSignal[i] == 1) {
                rPeaks.add(i);
            }
        }
        return rPeaks;
    }

    private static double[] filterEcg(double[] ecg) {
        // Cutoff frequency for the low pass filter (e.g. 15Hz)
        double alpha = 0.2;
        double[] filteredData = new double[ecg.length];
        filteredData[0] = ecg[0];
        for (int i = 1; i < ecg.length; i++) {
            filteredData[i] = alpha * filteredData[i - 1] + (1 - alpha) * ecg[i];
        }

        return filteredData;
    }

    private static double[] differentiateEcg(double[] filteredEcg) {
        double[] differentiatedEcg = new double[filteredEcg.length];
        for (int i = 1; i < filteredEcg.length; i++) {
            // Approximate the derivative using finite difference
            differentiatedEcg[i] = filteredEcg[i] - filteredEcg[i - 1];
        }
        return differentiatedEcg;
    }

    private static double[] squareEcg(double[] differentiatedEcg) {
        double[] squaredEcg = new double[differentiatedEcg.length];
        for (int i = 0; i < differentiatedEcg.length; i++) {
            // Apply the squaring operator to each element of the array
            squaredEcg[i] = differentiatedEcg[i] * differentiatedEcg[i];
        }
        return squaredEcg;
    }

    private static double[] integrateEcg(double[] squaredEcg) {
        double[] integratedEcg = new double[squaredEcg.length];
        for (int i = 0; i < squaredEcg.length; i++) {
            // Approximate the integral using the trapezoidal rule
            if (i == 0) {
                integratedEcg[i] = squaredEcg[i];
            } else {
                integratedEcg[i] = integratedEcg[i - 1] + (squaredEcg[i] + squaredEcg[i - 1]) / 2;
            }
        }
        return integratedEcg;
    }

    public static double[] applyThreshold(double[] differentiatedSignal) {
        double[] thresholdedSignal = new double[differentiatedSignal.length];
        double threshold = findThreshold(differentiatedSignal);
        System.out.println("Threshold " + threshold);
        for (int i = 0; i < differentiatedSignal.length; i++) {
            if (differentiatedSignal[i] > threshold) {
                thresholdedSignal[i] = 1;
            } else {
                thresholdedSignal[i] = 0;
            }
        }
        return thresholdedSignal;
    }

    public static double findThreshold(double[] differentiatedSignal) {
        double mean = 0;
        double std = 0;
        for (double val : differentiatedSignal) {
            mean += val;
        }
        mean /= differentiatedSignal.length;

        for (double val : differentiatedSignal) {
            std += Math.pow(val - mean, 2);
        }
        std = Math.sqrt(std / differentiatedSignal.length);
        return mean + std * 2;
    }

}



