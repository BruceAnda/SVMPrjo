package cn.zhaoliang5156.svmprjo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import cn.zhaoliang5156.svmprjo.svm.svm_predict
import cn.zhaoliang5156.svmprjo.svm.svm_scale
import cn.zhaoliang5156.svmprjo.svm.svm_train
import cn.zhaoliang5156.svmprjo.util.Features
import cn.zhaoliang5156.svmprjo.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import libsvm.svm
import libsvm.svm_model
import libsvm.svm_node
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.io.*

class MainActivity : AppCompatActivity() {

    lateinit var mSensorManager: SensorManager      // 传感器管理类
    lateinit var mAccSensor: Sensor             // 传感器
    var mHz = (1000.0 * 1000.0 / 32).toInt()        // 拿数据的时间

    val lableListener: AdapterView.OnItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(p0: AdapterView<*>?) {
            // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
            lable = p2
            tv_lable.text = "lable:${p2}"
        }

    }

    var lable = 0       // 要写入文件的lable标记
    var trainNum = 0        // 采集样本数量
    var currentCollectionTrainNum = 0   // 当前采集样本数据
    var isStartCollection = true    // 是否开始采集的标记

    // 采集的加速度监听器类
    val collectionSersorListener: SensorEventListener = object : SensorEventListener {

        var accArr = DoubleArray(128)
        var currentIndex = 0

        override fun onSensorChanged(event: SensorEvent) {
            var x = event.values[0]
            var y = event.values[1]
            var z = event.values[2]
            val sqrt = Math.sqrt((x * x + y * y + z * z).toDouble())
            tv_acc.text = "acc:${sqrt}"
            // 当不够128个数据的时候继续收集数据，够128个数据的时候写入文件
            if (currentIndex >= 128) {
                val dataToFeatures = Util.dataToFeatures(accArr, mHz)
                writeToFile(dataToFeatures)

                currentCollectionTrainNum++
                tv_collection_num.text = currentCollectionTrainNum.toString()
                currentIndex = 0

                if (currentCollectionTrainNum >= trainNum) {        // 已经采集够了
                    collection(false)
                    currentCollectionTrainNum = 0
                    currentIndex = 0
                    tv_collection_num.text = currentCollectionTrainNum.toString()
                }
            } else {
                accArr[currentIndex++] = sqrt
            }
        }

        /**
         * 把数据写入到文件
         */
        private fun writeToFile(dataToFeatures: Array<out String>) {
            var sb = StringBuilder()
            sb.append(lable)
            for (item in dataToFeatures) {
                sb.append(" ${item}")
            }
            sb.append("\n")
            trainFile.writeBytes(sb.toString())
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

        }
    }

    var isStartUnderStand: Boolean = true
    var underStandSensorListener = object : SensorEventListener {

        var accArr = DoubleArray(128)
        var currentIndex = 0

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSensorChanged(p0: SensorEvent) {
            var x = p0.values[0]
            var y = p0.values[1]
            var z = p0.values[2]
            val sqrt = Math.sqrt((x * x + y * y + z * z).toDouble())
            tv_acc.text = sqrt.toString()
            // 当够128个数据的时候开始识别，不够继续往double数组里写数据
            if (currentIndex >= 128) {
                val features = Util.dataToFeatures(accArr, mHz)
                val code = predictUnScaleData(features)
                tv_result.text = "识别结果:${lables[code.toInt()]}"
                currentIndex = 0
            } else {
                accArr[currentIndex++] = sqrt
            }
        }

        private fun predictUnScaleData(features: Array<out String>): Double {
            var px = arrayOfNulls<svm_node>(mFeatureCount)
            var p: svm_node
            for (i in 0..mFeatureCount - 1) {
                val tempNode = features[i].split(":")
                p = svm_node()
                p.index = tempNode[0].toInt()
                p.value = Features.zeroOneLibSvm(mScaleLower, mScaleUpper, tempNode[1].toDouble(), mFeatures[i][0], mFeatures[i][1])
                px[i] = p
                Log.i(javaClass.simpleName, "${i}mScaleLower:${mScaleLower}=====mScaleUpper:${mScaleUpper}=====tempNode0:${tempNode[0].toInt()}=====tempNode1:${tempNode[1].toDouble()}=====mFeatures0:${mFeatures[i][0]}=====mFeatures1:${mFeatures[i][1]}")
            }
            val code = svm.svm_predict(svm_load_model, px)
            return code
        }
    }

    var trainFileName = "train"
    var scaleFileName = "scale"
    var rangeFileName = "range"
    var modelFileName = "model"
    var predictFileName = "predict"
    var accuracyFileName = "accuracy"

    lateinit var trainFile: RandomAccessFile
    lateinit var lables: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lables = resources.getStringArray(R.array.Lable)

        trainFile = RandomAccessFile("${filesDir}/${trainFileName}", "rwd")

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager // 获取传感器管理类
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)     // 获取加速度传感器

        sp_lable.onItemSelectedListener = lableListener // 给lable设置监听器

        btn_collection.setOnClickListener {
            // 判读是否输入采集数量，如果输入就按数量进行采集，如果没有就提示输入
            val toString = et_train_num.text.toString()
            if (TextUtils.isEmpty(toString)) {
                et_train_num.setError("请输入采集数量")
            } else {
                trainNum = toString.toInt()
                collection(isStartCollection)
            }
        }

        // 开始训练按钮点击调用
        btn_train.setOnClickListener {
            doAsync {
                createScaleFile(arrayOf("-l", "0", "-u", "1", "-s", "${filesDir}/${rangeFileName}", "${filesDir}/${trainFileName}"))
                createModelFile(arrayOf("-s", "0", "-c", "128.0", "-t", "2", "-g", "8.0", "-e", "0.1", "${filesDir}/${scaleFileName}", "${filesDir}/${modelFileName}"))
                createPredictFile(arrayOf("${filesDir}/${scaleFileName}", "${filesDir}/${modelFileName}", "${filesDir}/${predictFileName}"))
                runOnUiThread {
                    var reader: BufferedReader = BufferedReader(InputStreamReader(FileInputStream("${filesDir}/${accuracyFileName}")))
                    val line = reader.readLine()
                    tv_accuracy.text = line
                }
            }
        }

        // 开始识别按钮点击调用
        btn_understand.setOnClickListener {
            understand(isStartUnderStand)
        }

        // 删除文件
        btn_delete.setOnClickListener {
            doAsync {
                val file = File("${filesDir}")
                for (item in file.list()) {
                    File("${filesDir}/${item}").delete()
                }
                runOnUiThread {
                    toast("删除成功！")
                }
            }
        }
    }

    /**
     * 创建归一化文件
     */
    fun createScaleFile(args: Array<String>) {
        val out = System.out
        var outScaleFile = PrintStream("${filesDir}/${scaleFileName}")
        System.setOut(outScaleFile)
        svm_scale.main(args)
        System.setOut(out)
    }

    /**
     * 创建model文件
     */
    fun createModelFile(args: Array<String>) {
        svm_train.main(args)
    }

    /**
     * 创建Predict文件
     */
    fun createPredictFile(args: Array<String>) {
        val out = System.out
        var outAccuracy = PrintStream("${filesDir}/${accuracyFileName}")
        System.setOut(outAccuracy)
        svm_predict.main(args)
        System.setOut(out)
    }

    /**
     * 采集数据
     */
    private fun collection(b: Boolean) {
        if (b) { // 开始采集
            trainFile.seek(trainFile.length())
            mSensorManager.registerListener(collectionSersorListener, mAccSensor, mHz)
            btn_collection.setImageResource(android.R.drawable.ic_media_pause)
        } else { // 停止采集
            mSensorManager.unregisterListener(collectionSersorListener)
            btn_collection.setImageResource(android.R.drawable.ic_media_play)
        }
        isStartCollection = !isStartCollection
    }

    lateinit var mFeatures: Array<DoubleArray>
    var mScaleLower: Double = 0.0
    var mScaleUpper: Double = 0.0
    var mFeatureCount = 0
    lateinit var svm_load_model: svm_model
    /**
     * 识别结果
     */
    private fun understand(b: Boolean) {
        if (b) { // 开始识别
            // 1. 读range文件
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream("${filesDir}/${rangeFileName}")))
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
            /*for (i in 0..mFeatureCount - 1) {
                Log.i(javaClass.simpleName, "mScaleLower:${mScaleLower}=====mScaleUpper:${mScaleUpper}=====mFeatures0:${mFeatures[i][0]}=====mFeatures1:${mFeatures[i][1]}")
            }*/
            // 2. 加载model文件
            svm_load_model = svm.svm_load_model("${filesDir}/${modelFileName}")

            mSensorManager.registerListener(underStandSensorListener, mAccSensor, mHz)
            btn_understand.setImageResource(android.R.drawable.ic_media_pause)
        } else { // 停止识别
            mSensorManager.unregisterListener(underStandSensorListener)
            btn_understand.setImageResource(android.R.drawable.ic_media_play)
        }
        isStartUnderStand = !isStartUnderStand
    }
}
