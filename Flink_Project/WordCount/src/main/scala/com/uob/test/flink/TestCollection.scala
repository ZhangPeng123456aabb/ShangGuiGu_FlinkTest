package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 15:14 
  */
import org.apache.flink.streaming.api.scala._
object TestCollection {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.获取数据源DataStream
    val dataStream:DataStream[String] = fsEnv.fromCollection(List("this is a demo","hello flink"))
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
        .print()
    fsEnv.execute("TestCollection")
  }
}
