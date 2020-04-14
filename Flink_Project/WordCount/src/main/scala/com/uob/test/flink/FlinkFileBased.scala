package com.uob.test.flink

import org.apache.flink.api.java.io.CsvOutputFormat
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.api.java.tuple.Tuple2
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.scala._
/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 17:43 
  */
object FlinkFileBased {
  def main(args: Array[String]): Unit = {
    //1.创建StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    val out = new CsvOutputFormat[Tuple2[String,Int]](new Path("file:///D:/flink-results"))
    //2.创建DataStream -细化
    val ds = fsEnv.socketTextStream("Spark",5555)
    ds.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .map(t=>new Tuple2(t._1,t._2))
      .writeUsingOutputFormat(out)
    fsEnv.execute("FlinkFileBased")
  }
}
