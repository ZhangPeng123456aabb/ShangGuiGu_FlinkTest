package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 16:19 
  */
import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
object FlinkAddSourceKafka {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.创建dataStream -细化
    val prop = new Properties()
    prop.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"Spark:9092")
    prop.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"g1")
    val flinkKafkaConsumer = new FlinkKafkaConsumer[String]("t1",new SimpleStringSchema(),prop)
    val dataStream:DataStream[String] = fsEnv.addSource(flinkKafkaConsumer)
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .print()
    fsEnv.execute("FlinkAddSourceKafka")
  }
}
