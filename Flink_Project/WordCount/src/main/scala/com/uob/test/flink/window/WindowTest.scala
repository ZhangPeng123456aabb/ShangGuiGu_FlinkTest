package com.uob.test.flink.window

import com.uob.test.flink.SensorReading
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.functions.{AssignerWithPeriodicWatermarks, AssignerWithPunctuatedWatermarks}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.time.Time
/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.window
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/22 - 16:22 
  */
object WindowTest {
  def main(args: Array[String]): Unit = {
    //配置执行环境
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    fsEnv.setParallelism(1)
    fsEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    fsEnv.getConfig.setAutoWatermarkInterval(100L)
    //创建输入流
    val dataStream = fsEnv.socketTextStream("Spark",5555)
    //数据转换
    //3.数据转换 -细化
    val outputStream = dataStream.map(
      data => {
        val dataArray = data.split(",")
        SensorReading(dataArray(0).trim, dataArray(1).toLong, dataArray(2).toDouble)
      })
      //.assignAscendingTimestamps(_.timestamp*1000)
      .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
      override def extractTimestamp(t: SensorReading): Long = t.timestamp*1000
    })

    val minTempPerWindowStream = outputStream
      .map(data => (data.id, data.temperature))
      .keyBy(_._1)
      .timeWindow(Time.seconds(15),Time.seconds(5)) //打开时间窗口
      .reduce((data1, data2) => (data1._1, data2._2.min(data2._2)))
    minTempPerWindowStream.print("min temp")

    outputStream.print("input stream")
    //启动执行
    fsEnv.execute("WindowTest")
  }

}
//class MyAssinger() extends AssignerWithPeriodicWatermarks[SensorReading]{
//  val bound = 60000
//  var maxTs = Long.MinValue
//  override def getCurrentWatermark: Watermark = new Watermark(maxTs-bound)
//
//  override def extractTimestamp(t: SensorReading, l: Long): Long = {
//    maxTs = maxTs.max(t.timestamp*1000)
//    t.timestamp*1000
//  }
//}
class MyAssinger() extends AssignerWithPunctuatedWatermarks[SensorReading]{
  override def checkAndGetNextWatermark(t: SensorReading, l: Long): Watermark = new Watermark(1)

  override def extractTimestamp(t: SensorReading, l: Long): Long = t.timestamp*1000
}