package com.uob.test.flink.window

import com.uob.test.flink.SensorReading
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
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
  * @date 2020/3/22 - 22:46
  */
object ProcessWindowFunction {
  def main(args: Array[String]): Unit = {
    //配置执行环境
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    fsEnv.setParallelism(1)
    fsEnv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    fsEnv.enableCheckpointing(60000)
    fsEnv.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    fsEnv.getCheckpointConfig.setCheckpointTimeout(100000)
    fsEnv.getCheckpointConfig.setFailOnCheckpointingErrors(false)
    //fsEnv.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    fsEnv.getCheckpointConfig.setMinPauseBetweenCheckpoints(100)
    fsEnv.getCheckpointConfig.enableExternalizedCheckpoints(ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION)
    //fsEnv.setRestartStrategy(RestartStrategies.fixedDelayRestart(3,500))
    fsEnv.setRestartStrategy(RestartStrategies.failureRateRestart(3,org.apache.flink.api.common.time.Time.seconds(300),org.apache.flink.api.common.time.Time.seconds(10)))
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
      .keyBy(_.id)
      .process(new TempIncrAlert())

    val processedStream2 = outputStream
      .keyBy(_.id)
      //.process(new TempChangeAlert(10.0))
        .flatMap(new TempChangeAlert(10.0))

    val processedStream3=outputStream
        .keyBy(_.id)
        .flatMapWithState[(String,Double,Double),Double]{
      //如果没有状态，也就是没有数据来过，就把当前温度值存入状态
      case (input:SensorReading,None) => (List.empty,Some(input.temperature))
        //如果有状态，就要与上次的温度值进行比较，如果大于阈值，就输出报警
      case (input:SensorReading,lastTemp:Some[Double]) =>
        val diff = (input.temperature-lastTemp.get).abs
        if(diff > 10.0){
          (List((input.id,lastTemp.get,input.temperature)),Some(input.temperature))
        }else{
          (List.empty,Some(input.temperature))
        }
    }


    outputStream.print("input stream")
    processedStream3.print("processed data")
    //启动执行
    fsEnv.execute("WindowTest")
  }
}
class TempIncrAlert() extends KeyedProcessFunction[String,SensorReading,String]{
  //定义一个状态，用来保存上一个数据的温度值
  lazy val lastTemp:ValueState[Double] = getRuntimeContext.getState(new ValueStateDescriptor[Double]("lastTemp",classOf[Double]))
  //定义一个状态，用来保存当前定时器的时间戳
  lazy val currentTimer:ValueState[Long]=getRuntimeContext.getState(new ValueStateDescriptor[Long]("currentTimer",classOf[Long]))
  override def processElement(i: SensorReading, context: KeyedProcessFunction[String, SensorReading, String]#Context, collector: Collector[String]): Unit = {
    //先取出上一个温度值
    val preTemp = lastTemp.value()
    //更新温度值
    lastTemp.update(i.temperature)
    val curTimers = currentTimer.value()
    //温度连续上升，并且没有设过定时器，则注册定时器
    if(i.temperature > preTemp && curTimers==0){
      val timerTs = context.timerService().currentProcessingTime()+10000L
      context.timerService().registerProcessingTimeTimer(timerTs)
      currentTimer.update(timerTs)
    }else if(preTemp > i.temperature || preTemp==0.0){
      //如果温度值下降，或是第一条数据，删除定时器，并并清空状态
      context.timerService().currentProcessingTime()
        //deleteProcessingTimeTimer(curTimers)
      currentTimer.clear()
    }
  }
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[String, SensorReading, String]#OnTimerContext, out: Collector[String]): Unit = {
    //输出报警信息
    out.collect(ctx.getCurrentKey+"温度连续上升")
    currentTimer.clear()
  }
}

class TempChangeAlert(threshold: Double) extends RichFlatMapFunction[SensorReading,(String,Double,Double)]{
  private var lastTempState:ValueState[Double]= _
  override def open(parameters: Configuration): Unit = {
    //初始化时声明state变量
   lastTempState = getRuntimeContext.getState(new ValueStateDescriptor[Double]("lastTemp",classOf[Double]))
  }
  override def flatMap(in: SensorReading, collector: Collector[(String, Double, Double)]): Unit = {
    //获取上次温度值
    val lastTemp = lastTempState.value()
    //用当前的温度值和上一次的求差，如果大于阙值，输出报警信息
    val diff = (in.temperature-lastTemp).abs
    if(diff>threshold){
      collector.collect((in.id,lastTemp,in.temperature))
    }
    lastTempState.update(in.temperature)
  }
}

class TempChangeAlert2(threshold: Double) extends KeyedProcessFunction[String,SensorReading,(String,Double,Double)]{
  lazy val lastTempState:ValueState[Double]=getRuntimeContext.getState(new ValueStateDescriptor[Double]("lastTemp",classOf[Double]))
  override def processElement(i: SensorReading, context: KeyedProcessFunction[String, SensorReading, (String, Double, Double)]#Context, collector: Collector[(String, Double, Double)]): Unit = {
    //获取上次温度值
    val lastTemp = lastTempState.value()
    //用当前的温度值和上一次的求差，如果大于阙值，输出报警信息
    val diff = (i.temperature-lastTemp).abs
    if(diff>threshold){
      collector.collect((i.id,lastTemp,i.temperature))
    }
    lastTempState.update(i.temperature)
  }
}
