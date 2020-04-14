package com.uob.test.flink

import java.util.Properties
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 17:04 
  */
object FlinkDefineKafkaDeserialization {
  def main(args: Array[String]): Unit = {
    //1.配置StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.创建dataStream
    val props = new Properties()
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"Spark:9092")
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"g1")
    val flinkKafkaConsumer = new FlinkKafkaConsumer[(Int,Long,String,String,String)]("t1",new UserDefinekafkaDeserializationSchema(),props)
    val dataStream = fsEnv.addSource(flinkKafkaConsumer)
    dataStream.print()
    fsEnv.execute("FlinkDefineKafkaDeserialization")
  }
}
