package com.uob.test.flink.sinktest

import java.util.Properties

import com.uob.test.flink.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend
import org.apache.flink.runtime.state.filesystem.FsStateBackend
import org.apache.flink.runtime.state.memory.MemoryStateBackend
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.kafka.clients.consumer.ConsumerConfig

/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.sinktest
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/20 - 9:14 
  */
import org.apache.flink.streaming.api.scala._
object KafkaSinkTest {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    fsEnv.setParallelism(1)
    fsEnv.enableCheckpointing(1000L)
   // fsEnv.setStateBackend(new RocksDBStateBackend(""))
    //读入数据
    //val inputStream = fsEnv.socketTextStream("Spark",5555)
    //2.创建DataStream -细化
    val props = new Properties()
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"Spark:9092")
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"g1")
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest")
    val dataStream = fsEnv.addSource(new FlinkKafkaConsumer[String]("sensor",new SimpleStringSchema(),props))
    //3.数据转换 -细化
    val outStram = dataStream.map(data => {
      val dataArray = data.split(",")
      SensorReading(dataArray(0).trim, dataArray(1).trim.toLong, dataArray(2).trim.toDouble).toString //方便序列化
    })
      outStram.print()
      //sink
    outStram.addSink(new FlinkKafkaProducer[String]("Spark:9092", "KafkaSink", new SimpleStringSchema()))

    fsEnv.execute("kafka sink test")
  }
}
