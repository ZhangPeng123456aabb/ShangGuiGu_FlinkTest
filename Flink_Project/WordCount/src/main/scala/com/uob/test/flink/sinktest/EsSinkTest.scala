package com.uob.test.flink.sinktest

import java.util
import java.util.Properties

import com.uob.test.flink.SensorReading
import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.http.HttpHost
import org.elasticsearch.client.{Request, Requests}
/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.sinktest
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/20 - 10:49 
  */
object EsSinkTest {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    fsEnv.setParallelism(1)
    //读入数据
    //val inputStream = fsEnv.socketTextStream("Spark",5555)
    //2.创建DataStream -细化
    val props = new Properties()
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"Spark:9092")
    props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"g1")
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer")
    props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest")
    props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false")
    val dataStream = fsEnv.addSource(new FlinkKafkaConsumer[String]("sensor",new SimpleStringSchema(),props))
    //3.数据转换 -细化
    val outputStream = dataStream.map(
      data => {
        val dataArray = data.split(",")
        SensorReading(dataArray(0).trim, dataArray(1).toLong, dataArray(2).toDouble)
      }
    )
    val httpHosts = new util.ArrayList[HttpHost]()
    httpHosts.add(new HttpHost("192.168.182.144",9200,"http"))
    //创建一个esSink的builder
    val esSinkBuilder = new ElasticsearchSink.Builder[SensorReading](
      httpHosts,
      new ElasticsearchSinkFunction[SensorReading]{
        override def process(t: SensorReading, runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
          println("saving data"+t)
          //包装成一个Map或者JsonObject
          val json = new util.HashMap[String,String]()
          json.put("sensor_id",t.id)
          json.put("temperature",t.temperature.toString)
          json.put("timestamp",t.timestamp.toString)
          //创建index,request,准备发送数据
          val indexRequest = Requests.indexRequest()
            .index("sensor")
            .`type`("readingData")
            .source(json)
          //利用index发送请求，引入数据
          requestIndexer.add(indexRequest)
          println("data saved.")
        }
      }
    )
    //sink
    outputStream.addSink(esSinkBuilder.build())
    //3.执行
    fsEnv.execute("EsSinkTest")
  }
}
