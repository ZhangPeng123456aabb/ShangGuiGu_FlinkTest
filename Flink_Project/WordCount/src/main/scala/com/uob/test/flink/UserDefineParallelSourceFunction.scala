package com.uob.test.flink

import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}

import scala.util.Random


/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 15:24 
  */
class UserDefineParallelSourceFunction extends ParallelSourceFunction[String] {
  val lines=Array("this is a demo","hello world","hello flink")
  var isRunning = true
  override def run(sourceContext: SourceFunction.SourceContext[String]): Unit = {
    while(isRunning){
      Thread.sleep(1000)
      sourceContext.collect(lines(new Random().nextInt(lines.length)))
    }
  }

  override def cancel(): Unit = {
    isRunning = false
  }
}
