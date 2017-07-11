package cn.zhaoliang5156.svmprjo.util;

import android.os.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.io.File.separator;

/**
 * Created by zhaoliang on 16/10/16.
 */

public class Util {

    /**
     * 特征lable
     */
    private static final int FUN_1_MINIMUM_LABLE = 1;
    private static final int FUN_2_MAXIMUM_LABLE = 2;
    private static final int FUN_3_MEANCROSSINGSRATE_LABLE = 3;
    private static final int FUN_4_STANDARDDEVIATION_LABLE = 4;
    private static final int FUN_5_SPP_LABLE = 5;
    private static final int FUN_6_ENERGY_LABLE = 6;
    private static final int FUN_7_ENTROPY_LABLE = 7;
    private static final int FUN_8_CENTROID_LABLE = 8;
    private static final int FUN_9_MEAN_LABLE = 9;
    private static final int FUN_10_RMS_LABLE = 10;
    private static final int FUN_11_SMA_LABLE = 11;
    private static final int FUN_12_IQR_LABLE = 12;
    private static final int FUN_13_MAD_LABLE = 13;
    private static final int FUN_14_TENERGY_LABLE = 14;
    private static final int FUN_15_FDEV_LABLE = 15;
    private static final int FUN_16_FMEAN_LABLE = 16;
    private static final int FUN_17_SKEW_LABLE = 17;
    private static final int FUN_18_KURT_LABLE = 18;
    private static final int FUN_19_MEDIAN_LABLE = 19;


    /**
     * 把double数组转换成特征字符串数组
     *
     * @param doubleArr 数据
     * @param sinter    采样间隔(毫秒数)
     * @return
     */
    public static String[] dataToFeatures(double[] doubleArr, int sinter) {
        List<String> featuresList = new ArrayList<>();
        double[] fft = Features.fft(doubleArr.clone());
        featuresList.add(FUN_1_MINIMUM_LABLE + ":" + Features.minimum(doubleArr.clone()));
        featuresList.add(FUN_2_MAXIMUM_LABLE + ":" + Features.maximum(doubleArr.clone()));
        featuresList.add(FUN_3_MEANCROSSINGSRATE_LABLE + ":" + Features.meanCrossingsRate(doubleArr.clone()));
        featuresList.add(FUN_4_STANDARDDEVIATION_LABLE + ":" + Features.standardDeviation(doubleArr.clone()));
        featuresList.add(FUN_5_SPP_LABLE + ":" + Features.spp(fft.clone()));
        featuresList.add(FUN_6_ENERGY_LABLE + ":" + Features.energy(fft.clone()));
        featuresList.add(FUN_7_ENTROPY_LABLE + ":" + Features.entropy(fft.clone()));
        featuresList.add(FUN_8_CENTROID_LABLE + ":" + Features.centroid(fft.clone()));
        featuresList.add(FUN_9_MEAN_LABLE + ":" + Features.mean(doubleArr.clone()));
        featuresList.add(FUN_10_RMS_LABLE + ":" + Features.rms(doubleArr.clone()));
        featuresList.add(FUN_11_SMA_LABLE + ":" + Features.sma(doubleArr.clone(), sinter / 1000.0));
        featuresList.add(FUN_12_IQR_LABLE + ":" + Features.iqr(doubleArr.clone()));
        featuresList.add(FUN_13_MAD_LABLE + ":" + Features.mad(doubleArr.clone()));
        featuresList.add(FUN_14_TENERGY_LABLE + ":" + Features.tenergy(doubleArr.clone()));
        featuresList.add(FUN_15_FDEV_LABLE + ":" + Features.fdev(fft.clone()));
        featuresList.add(FUN_16_FMEAN_LABLE + ":" + Features.fmean(fft.clone()));
        featuresList.add(FUN_17_SKEW_LABLE + ":" + Features.skew(fft.clone()));
        featuresList.add(FUN_18_KURT_LABLE + ":" + Features.kurt(fft.clone()));
        featuresList.add(FUN_19_MEDIAN_LABLE + ":" + Features.median(doubleArr.clone()));
        return listToString(featuresList);
    }

    private static String[] listToString(List<String> featuresList) {
        String[] strings = new String[featuresList.size()];
        for (int i = 0; i < featuresList.size(); i++) {
            strings[i] = featuresList.get(i);
        }
        return strings;
    }
}
