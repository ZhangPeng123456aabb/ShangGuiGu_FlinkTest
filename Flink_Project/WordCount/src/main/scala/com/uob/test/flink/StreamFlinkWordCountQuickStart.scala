package com.uob.test.flink

/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 14:57 
  */
import org.apache.flink.api.common.io.FilePathFilter
import org.apache.flink.api.java.io.TextInputFormat
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.functions.source.FileProcessingMode
import org.apache.flink.streaming.api.scala._
object StreamFlinkWordCountQuickStart {
  def main(args: Array[String]): Unit = {
    //1.配置StreamExecutionEnvironment
    val fsEnv = StreamExecutionEnvironment.getExecutionEnvironment
    //2.获取数据源
    val filePath="file:///D:\\Data1"
    val format = new TextInputFormat(null)
    format.setFilesFilter(new FilePathFilter {
      override def filterPath(path: Path): Boolean = {
        if(path.getName.startsWith("1")){
          //过滤不符合的文件
          return true
        }
        false
      }
    })
    val dataStream:DataStream[String] = fsEnv.readFile(format,filePath,FileProcessingMode.PROCESS_CONTINUOUSLY,1000)
    //对数据做转换
    dataStream.flatMap(_.split("\\s+"))
      .map((_,1))
      .keyBy(0)
      .sum(1)
      .print()
    fsEnv.execute("StreamFlinkWordCountQuickStart")

  }
}
