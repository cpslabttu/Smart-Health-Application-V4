package com.example.cps_lab.app;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;

public class WelchMethod {

    public static double[] welchPSD(List<Double> signal, int segmentLength, int overlap, double samplingRate) {
        int step = segmentLength - overlap;
        int numSegments = (signal.size() - overlap) / step;

        int fftLength = nextPowerOfTwo(segmentLength);
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        double[] window = hanningWindow(segmentLength);
        double[] psd = new double[fftLength / 2 + 1];

        for (int i = 0; i < numSegments; i++) {
            double[] segment = new double[fftLength];
            for (int j = 0; j < segmentLength; j++) {
                segment[j] = signal.get(i * step + j);
            }

            for (int j = 0; j < segmentLength; j++) {
                segment[j] *= window[j];
            }

            // Zero-padding if the segment length is less than fftLength
            for (int j = segmentLength; j < fftLength; j++) {
                segment[j] = 0.0;
            }

            Complex[] fftResult = fft.transform(segment, TransformType.FORWARD);

            for (int j = 0; j < psd.length; j++) {
                psd[j] += Math.pow(fftResult[j].abs(), 2);
            }
        }

        for (int i = 0; i < psd.length; i++) {
            psd[i] /= (numSegments * windowPower(window));
        }

        double[] peakFrequencyandBandwidth = findPeakFrequencyandBandwidth(psd, samplingRate, fftLength);
        return peakFrequencyandBandwidth;
    }

    private static double[] hanningWindow(int length) {
        double[] window = new double[length];
        for (int i = 0; i < length; i++) {
            window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (length - 1)));
        }
        return window;
    }

    private static double windowPower(double[] window) {
        double power = 0;
        for (double w : window) {
            power += w * w;
        }
        return power / window.length;
    }

    private static int nextPowerOfTwo(int n) {
        return (int) Math.pow(2, Math.ceil(Math.log(n) / Math.log(2)));
    }

    public static double[] findPeakFrequencyandBandwidth(double[] psd, double samplingRate, int fftLength) {
        double maxPower = -1;
        int maxIndex = -1;
        double minPower = Double.MAX_VALUE;
        int minIndex = Integer.MAX_VALUE;
        int[] maxIndexArr = new int[1000];

        for (int i = 0; i < psd.length; i++) {
            if(psd[i] < minPower){
                minIndex = i;
                minPower = psd[i];
            }
            if(psd[i] > minPower){
                break;
            }
        }

        int lastMinIndex = minIndex;
        int k = 0;
        boolean status = false;
        for(int i=minIndex;i<psd.length;i++) {
            if (psd[i] > maxPower) {
                maxPower = psd[i];
                maxIndex = i;
                status = true;
            }
            if(psd[i] < maxPower && status && k < 1000){
                maxIndexArr[k] = maxIndex;
                status = false;
                k++;
            }
        }

        for(int i=0;i<maxIndexArr.length;i++){
            if(maxIndexArr[i] != 0 && maxIndexArr[i] < 40) {
                //System.out.println(maxIndexArr[i]);
                maxIndex = maxIndexArr[i];
                //break;
            }
        }

        for(int i=maxIndex;i>=minIndex;i--){
            if(psd[i] < maxPower){
                lastMinIndex = i;
                maxPower = psd[i];
                if(i-1 >= 0 && psd[i-1] > maxPower){
                    break;
                }
            }
        }

//        System.out.println(minPower + " MIN " + minIndex);
//
//        System.out.println(maxPower + " MAX " + maxIndex);
//
//        System.out.println("Last Min " + lastMinIndex);

        // Calculate the corresponding frequency
        double peakFrequency = (double) maxIndex * samplingRate / fftLength;
        double bandwidth = (double) ((maxIndex * samplingRate / fftLength) - (lastMinIndex * samplingRate / fftLength));
        return new double[]{peakFrequency, bandwidth};
    }

}
