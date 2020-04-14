package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 14:14 
  */
import org.apache.flink.streaming.api.scala._
object FlinkWordCountQuickStart {
  def main(args: Array[String]): Unit = {
    //创建ExecutionEnvironment执行环境
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.创建DataStream -细化
    val filePath="file:///D:\\data"
    val dataStream:DataStream[String]= fsEnv.readTextFile(filePath)
    //3.对数据做转换
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .print()
    fsEnv.execute("FlinkWordCountsQuickStart")
  }
}
