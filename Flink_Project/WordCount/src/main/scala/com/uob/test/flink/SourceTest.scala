package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/19 - 17:19 
  */
import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig

import scala.util.Random
case class SensorReading(id:String,timestamp: Long,temperature:Double)
object SourceTest {
  def main(args: Array[String]): Unit = {
    //1.配置StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.创建DataStream -细化
    val props = new Properties()
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"Spark:9092")
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"g1")
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest")
    val dataStream = fsEnv.addSource(new FlinkKafkaConsumer[String]("sensor",new SimpleStringSchema(),props))
    //3.自定义Source
    val dataStream1 = fsEnv.addSource(new SensorSource())
    dataStream1.print("dataStream1").setParallelism(1)
    fsEnv.execute("SourceTest")

  }
}
class SensorSource() extends SourceFunction[SensorReading]{
  var isRunning=true
  override def run(sourceContext: SourceFunction.SourceContext[SensorReading]): Unit = {
    //初始化一个随机数生成器
    val rand=new Random()
    //初始化定义一个传感器温度数据
    var curTemp = 1.to(10).map(
      i => ("sensor_"+i,60+rand.nextGaussian()*20)
    )
    //产生数据流
    while(isRunning){
      //在前一次的基础上更新温度值
      curTemp=curTemp.map(
        t => (t._1,t._2+rand.nextGaussian())
      )
      //获取当前时间
      val curTime = System.currentTimeMillis()
      curTemp.foreach(
        t => sourceContext.collect(SensorReading(t._1,curTime,t._2))
      )
      //设定时间间隔
      Thread.sleep(500)
    }
  }

  override def cancel(): Unit = {
    isRunning=false
  }
}
