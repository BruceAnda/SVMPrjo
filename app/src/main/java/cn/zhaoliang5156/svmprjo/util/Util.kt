package cn.zhaoliang5156.svmprjo.util

import android.util.Log
import libsvm.svm
import libsvm.svm_model
import libsvm.svm_node
import java.io.*
import java.util.ArrayList

/**
 * Created by zhaoliang on 16/10/16.
 */

object Util {

    /**
     * 特征lable
     */
    private val FUN_1_MINIMUM_LABLE = 1
    private val FUN_2_MAXIMUM_LABLE = 2
    private val FUN_3_MEANCROSSINGSRATE_LABLE = 3
    private val FUN_4_STANDARDDEVIATION_LABLE = 4
    private val FUN_5_SPP_LABLE = 5
    private val FUN_6_ENERGY_LABLE = 6
    private val FUN_7_ENTROPY_LABLE = 7
    private val FUN_8_CENTROID_LABLE = 8
    private val FUN_9_MEAN_LABLE = 9
    private val FUN_10_RMS_LABLE = 10
    private val FUN_11_SMA_LABLE = 11
    private val FUN_12_IQR_LABLE = 12
    private val FUN_13_MAD_LABLE = 13
    private val FUN_14_TENERGY_LABLE = 14
    private val FUN_15_FDEV_LABLE = 15
    private val FUN_16_FMEAN_LABLE = 16
    private val FUN_17_SKEW_LABLE = 17
    private val FUN_18_KURT_LABLE = 18
    private val FUN_19_MEDIAN_LABLE = 19


    /**
     * 把double数组转换成特征字符串数组

     * @param doubleArr 数据
     * *
     * @param sinter    采样间隔(毫秒数)
     * *
     * @return
     */
    fun dataToFeatures(doubleArr: DoubleArray, sinter: Int): Array<String?> {
        val featuresList = ArrayList<String>()
        val fft = Features.fft(doubleArr.clone())
        featuresList.add(FUN_1_MINIMUM_LABLE.toString() + ":" + Features.minimum(doubleArr.clone()))
        featuresList.add(FUN_2_MAXIMUM_LABLE.toString() + ":" + Features.maximum(doubleArr.clone()))
        featuresList.add(FUN_3_MEANCROSSINGSRATE_LABLE.toString() + ":" + Features.meanCrossingsRate(doubleArr.clone()))
        featuresList.add(FUN_4_STANDARDDEVIATION_LABLE.toString() + ":" + Features.standardDeviation(doubleArr.clone()))
        featuresList.add(FUN_5_SPP_LABLE.toString() + ":" + Features.spp(fft.clone()))
        featuresList.add(FUN_6_ENERGY_LABLE.toString() + ":" + Features.energy(fft.clone()))
        featuresList.add(FUN_7_ENTROPY_LABLE.toString() + ":" + Features.entropy(fft.clone()))
        featuresList.add(FUN_8_CENTROID_LABLE.toString() + ":" + Features.centroid(fft.clone()))
        featuresList.add(FUN_9_MEAN_LABLE.toString() + ":" + Features.mean(doubleArr.clone()))
        featuresList.add(FUN_10_RMS_LABLE.toString() + ":" + Features.rms(doubleArr.clone()))
        featuresList.add(FUN_11_SMA_LABLE.toString() + ":" + Features.sma(doubleArr.clone(), sinter / 1000.0))
        featuresList.add(FUN_12_IQR_LABLE.toString() + ":" + Features.iqr(doubleArr.clone()))
        featuresList.add(FUN_13_MAD_LABLE.toString() + ":" + Features.mad(doubleArr.clone()))
        featuresList.add(FUN_14_TENERGY_LABLE.toString() + ":" + Features.tenergy(doubleArr.clone()))
        featuresList.add(FUN_15_FDEV_LABLE.toString() + ":" + Features.fdev(fft.clone()))
        featuresList.add(FUN_16_FMEAN_LABLE.toString() + ":" + Features.fmean(fft.clone()))
        featuresList.add(FUN_17_SKEW_LABLE.toString() + ":" + Features.skew(fft.clone()))
        featuresList.add(FUN_18_KURT_LABLE.toString() + ":" + Features.kurt(fft.clone()))
        featuresList.add(FUN_19_MEDIAN_LABLE.toString() + ":" + Features.median(doubleArr.clone()))
        return listToString(featuresList)
    }

    private fun listToString(featuresList: List<String>): Array<String?> {
        val strings = arrayOfNulls<String>(featuresList.size)
        for (i in featuresList.indices) {
            strings[i] = featuresList[i]
        }
        return strings
    }

    /**
     * 把数据写入到文件
     * @param path
     * *
     * @param lable
     * *
     * @param features
     */
    fun writeToFile(path: String, lable: Int, features: Array<String?>) {
        var bufferedWriter: BufferedWriter? = null
        try {
            bufferedWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(path, true)))
            val stringBuilder = StringBuilder()
            stringBuilder.append(lable)
            for (feature in features) {
                stringBuilder.append(" " + feature)
            }
            stringBuilder.append("\n")
            bufferedWriter.write(stringBuilder.toString())
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                bufferedWriter!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    lateinit var mFeatures: Array<DoubleArray>
    var mScaleLower: Double = 0.0
    var mScaleUpper: Double = 0.0
    var mFeatureCount = 0
    lateinit var svm_load_model: svm_model

    /**
     * 读入range文件
     */
    fun readRange(path: String) {
        val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(path)))
        var feature = bufferedReader.readLine()
        val range = ArrayList<String>()
        while (feature != null) {
            range.add(feature)
            feature = bufferedReader.readLine()
        }
        mFeatureCount = range.size - 2
        mFeatures = Array(mFeatureCount) { kotlin.DoubleArray(2) }
        var lowerAndUpper = range[1]
        val split = lowerAndUpper.split(" ")
        mScaleLower = split[0].toDouble()
        mScaleUpper = split[1].toDouble()
        for (i in 0..mFeatureCount - 1) {
            val featuresLowerAndUpper = range[i + 2].split(" ")
            mFeatures[i][0] = featuresLowerAndUpper[1].toDouble()
            mFeatures[i][1] = featuresLowerAndUpper[2].toDouble()
        }
    }

    /**
     * 加载model文件
     */
    fun loadModel(path: String) {
        svm_load_model = svm.svm_load_model(path)
    }

    /**
     * 加载文件
     */
    fun loadFile(rangeFile: String, modelFile: String) {
        readRange(rangeFile)
        loadModel(modelFile)
    }

    /**
     * 预测没有归一化的文件
     */
    fun predictUnScaleData(features: Array<String?>): Double {
        var px = arrayOfNulls<svm_node>(mFeatureCount)
        var p: svm_node
        for (i in 0..mFeatureCount - 1) {
            val tempNode = features[i]?.split(":")
            p = svm_node()
            p.index = tempNode!![0].toInt()
            p.value = Features.zeroOneLibSvm(mScaleLower, mScaleUpper, tempNode[1].toDouble(), mFeatures[i][0], mFeatures[i][1])
            px[i] = p
            Log.i(javaClass.simpleName, "${i}mScaleLower:${mScaleLower}=====mScaleUpper:${mScaleUpper}=====tempNode0:${tempNode[0].toInt()}=====tempNode1:${tempNode[1].toDouble()}=====mFeatures0:${mFeatures[i][0]}=====mFeatures1:${mFeatures[i][1]}")
        }
        val code = svm.svm_predict(svm_load_model, px)
        return code
    }
}
