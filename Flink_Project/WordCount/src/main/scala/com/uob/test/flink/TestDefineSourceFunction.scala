package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 15:33 
  */
import org.apache.flink.streaming.api.scala._
object TestDefineSourceFunction {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.创建DataStream
    val dataStream:DataStream[String] = fsEnv.addSource(new UserDefineParallelSourceFunction)
    dataStream.setParallelism(10)
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .print()
    fsEnv.execute("TestDefineSourceFunction")
  }
}
