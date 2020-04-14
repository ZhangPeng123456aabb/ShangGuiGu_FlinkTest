package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 14:36 
  */
import org.apache.flink.api.java.io.TextInputFormat
import org.apache.flink.streaming.api.scala._
object FlinkWordCountQuickStart2 {
  def main(args: Array[String]): Unit = {
    //1.配置StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.获取数据源
    val filePath="file:///D:\\Data1"
    val format = new TextInputFormat(null)
    val dataStream:DataStream[String] = fsEnv.readFile(format,filePath)
    //3.处理数据源
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .setParallelism(2)
      .keyBy(0)
      .sum(1)
      .print()
    fsEnv.execute("FlinkWordCountQuickStart2")
  }
}
