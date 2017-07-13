package cn.zhaoliang5156.svmprjo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import cn.zhaoliang5156.svmprjo.svm.svm_predict
import cn.zhaoliang5156.svmprjo.svm.svm_scale
import cn.zhaoliang5156.svmprjo.svm.svm_train
import cn.zhaoliang5156.svmprjo.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.io.*

/**
 * 步态分析
 */
class MainActivity : AppCompatActivity() {

    lateinit var mSensorManager: SensorManager      // 传感器管理类
    lateinit var mAccSensor: Sensor             // 传感器
    var mHz = (1000.0 * 1000.0 / 32).toInt()        // 拿数据的时间

    var lable = 0       // 要写入文件的lable标记
    var trainNum = 0        // 采集样本数量
    var currentCollectionTrainNum = 0   // 当前采集样本数据
    var isStartCollection = false    // 是否开始采集的标记
    var isStartUnderStand: Boolean = false

    // 采集的加速度监听器类
    val sensorListener: SensorEventListener = object : SensorEventListener {

        var accArr = DoubleArray(128)
        var currentIndex = 0

        override fun onSensorChanged(event: SensorEvent) {
            var x = event.values[0]
            var y = event.values[1]
            var z = event.values[2]
            val sqrt = Math.sqrt((x * x + y * y + z * z).toDouble())
            tv_acc.text = "${sqrt}"
            // 当不够128个数据的时候继续收集数据，够128个数据的时候写入文件
            if (currentIndex >= 128) {
                val features = Util.dataToFeatures(accArr, mHz)

                if (isStartCollection) {  // 如果是开始收集数据走到这里，就存起来
                    Util.writeToFile("${filesDir}/train", lable, features)
                    currentCollectionTrainNum++
                    tv_collection_num.text = currentCollectionTrainNum.toString()

                    if (currentCollectionTrainNum >= trainNum) {        // 已经采集够了
                        collection(isStartCollection)
                        currentCollectionTrainNum = 0
                        tv_collection_num.text = "已经采集:${currentCollectionTrainNum}"
                    }
                } else {     // 否则则是识别
                    val code = Util.predictUnScaleData(features)
                    result(code.toInt())
                }
                currentIndex = 0

            } else {
                accArr[currentIndex++] = sqrt
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 选择采集样本数量
        rg_train_num.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.rb_train_num_one -> trainNum = 30
                R.id.rb_train_num_two -> trainNum = 50
                R.id.rb_train_num_three -> trainNum = 100
            }
            tv_train_num.text = "采集样本数量${trainNum}"
        }
        rg_train_num.check(R.id.rb_train_num_one)   // 设置默认选中30

        // 选中标记lable
        rg_lable.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.rb_lable_one -> lable = 0
                R.id.rb_lable_two -> lable = 1
                R.id.rb_lable_three -> lable = 2
            }
            tv_lable.text = "lable:${lable}"
        }
        rg_lable.check(R.id.rb_lable_one)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager // 获取传感器管理类
        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)     // 获取加速度传感器

        btn_collection.setOnClickListener {
            collection(isStartCollection)
        }

        // 开始训练按钮点击调用
        btn_train.setOnClickListener {
            btn_train.setBackgroundResource(R.mipmap.train)
            doAsync {
                createScaleFile(arrayOf("-l", "0", "-u", "1", "-s", "${filesDir}/range", "${filesDir}/train"))
                createModelFile(arrayOf("-s", "0", "-c", "128.0", "-t", "2", "-g", "8.0", "-e", "0.1", "${filesDir}/scale", "${filesDir}/model"))
                createPredictFile(arrayOf("${filesDir}/scale", "${filesDir}/model", "${filesDir}/predict"))
                runOnUiThread {
                    var reader: BufferedReader = BufferedReader(InputStreamReader(FileInputStream("${filesDir}/accuracy")))
                    val line = reader.readLine()
                    tv_accuracy.text = line.replace("Accuracy", "准确率")
                    btn_train.setBackgroundResource(R.mipmap.train_off)
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
        var outScaleFile = PrintStream("${filesDir}/scale")
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
        var outAccuracy = PrintStream("${filesDir}/accuracy")
        System.setOut(outAccuracy)
        svm_predict.main(args)
        System.setOut(out)
    }

    /**
     * 采集数据
     */
    private fun collection(b: Boolean) {
        if (!b) { // 开始采集
            mSensorManager.registerListener(sensorListener, mAccSensor, mHz)
            btn_collection.setBackgroundResource(R.mipmap.sample)
        } else { // 停止采集
            mSensorManager.unregisterListener(sensorListener)
            btn_collection.setBackgroundResource(R.mipmap.sample_off)
        }
        isStartCollection = !isStartCollection
    }

    /**
     * 识别结果
     */
    private fun understand(b: Boolean) {
        if (!b) { // 开始识别
            Util.loadFile("${filesDir}/range", "${filesDir}/model")
            mSensorManager.registerListener(sensorListener, mAccSensor, mHz)
            btn_understand.setBackgroundResource(R.mipmap.test)
        } else { // 停止识别
            mSensorManager.unregisterListener(sensorListener)
            btn_understand.setBackgroundResource(R.mipmap.test_off)
            result(-1)
        }
        isStartUnderStand = !isStartUnderStand
    }

    fun result(code: Int) {
        iv_still.setBackgroundResource(R.mipmap.gait_still_off)
        iv_walk.setBackgroundResource(R.mipmap.gait_walk_off)
        iv_run.setBackgroundResource(R.mipmap.gait_run_off)
        when (code) {
            0 -> iv_still.setBackgroundResource(R.mipmap.gait_still)
            1 -> iv_walk.setBackgroundResource(R.mipmap.gait_walk)
            2 -> iv_run.setBackgroundResource(R.mipmap.gait_run)
        }
    }
}