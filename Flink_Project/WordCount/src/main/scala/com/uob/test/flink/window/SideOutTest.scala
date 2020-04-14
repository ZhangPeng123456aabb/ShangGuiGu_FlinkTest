package com.uob.test.flink.window

import com.uob.test.flink.SensorReading
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.window
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/22 - 23:51 
  */
object SideOutTest {
  def main(args: Array[String]): Unit = {
    //配置执行环境
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    fsEnv.setParallelism(1)
    fsEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
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
    val processedStream = outputStream
      .process(new FreezingAlert())
    processedStream.print("processed data")
    processedStream.getSideOutput(new OutputTag[String]("freezing alert")).print("alert data")
    //启动执行
    fsEnv.execute("WindowTest")
  }
}
//冰点报警，如果小于32F,输出报警信息到输出流
class FreezingAlert() extends ProcessFunction[SensorReading,SensorReading]{
  lazy val alertOutput:OutputTag[String]=new  OutputTag[String]("freezing alert")
  override def processElement(i: SensorReading, context: ProcessFunction[SensorReading, SensorReading]#Context, collector: Collector[SensorReading]): Unit = {
    if(i.temperature<32.0){
      context.output(alertOutput,"freezing alert for "+i.id)
    }else{
      collector.collect(i)
    }
  }
}