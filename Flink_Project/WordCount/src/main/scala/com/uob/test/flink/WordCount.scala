package com.uob.test.flink

import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.scala._
/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 13:27 
  */
object WordCount {
  def main(args: Array[String]): Unit = {
    //创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //创建DataStream -细化
    val dataStream:DataStream[String] = fsEnv.socketTextStream("Spark",5555)
    //对数据进行转换 -operator
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .print()
    //执行计算
    fsEnv.execute("WordCount")
  }
}
