package com.uob.test.flink.sinktest

import java.util.Properties

import com.uob.test.flink.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.sinktest
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/20 - 10:10 
  */
object RedisSinkTest {
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
    val dataStream = fsEnv.addSource(new FlinkKafkaConsumer[String]("sensor",new SimpleStringSchema(),props))

    //transform
    val outputStream = dataStream.map(
      data => {
        val dataArray = data.split(",")
        SensorReading(dataArray(0).trim, dataArray(1).toLong, dataArray(2).toDouble)
      }
    )
    val conf = new FlinkJedisPoolConfig.Builder()
      .setHost("Spark")
      .setPort(6379)
      .build()
    outputStream.addSink(new RedisSink(conf,new MyRedisMapper))
    fsEnv.execute("RedisSinkTest")
  }
}

class MyRedisMapper() extends RedisMapper[SensorReading]{
  //正在保存数据到redis的命令
  override def getCommandDescription: RedisCommandDescription = {
    //把传感器的id值和温度值保存到哈希表 HSET key/field value
    new RedisCommandDescription(RedisCommand.HSET,"sensor_temperature")

  }
   //保存到redis的key
  override def getKeyFromData(t: SensorReading): String =t.temperature.toString
  //保存到redis的value
  override def getValueFromData(t: SensorReading): String = t.id
}