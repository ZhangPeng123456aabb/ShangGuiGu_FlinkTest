package com.uob.test.flink.sinktest

import java.sql.{Connection, DriverManager, PreparedStatement}
import java.util.Properties

import com.uob.test.flink.SensorReading
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
/**
  * @ProjectName Flink_Project
  * @PackageName com.uob.test.flink.sinktest
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/20 - 14:13 
  */
object JdbcSinkTest {
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
    val InputDataStream = fsEnv.addSource(new FlinkKafkaConsumer[String]("sensor",new SimpleStringSchema(),props))
    //3.数据转换 -细化
    val OutputDataStream = InputDataStream.map(
      data => {
        val dataArray = data.split(",")
        SensorReading(dataArray(0).trim, dataArray(1).toLong, dataArray(2).toDouble)
      }
    )
    //Sink
    OutputDataStream.addSink(new MyJdbcSink)
     fsEnv.execute("JdbcSinkTest")
  }
}
class MyJdbcSink() extends RichSinkFunction[SensorReading]{
  //定义sql连接、预编译器
  var conn:Connection=_
  var insertStmt:PreparedStatement=_
  var updateStmt:PreparedStatement=_
  //初始化，创建连接和预编译语句
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    conn=DriverManager.getConnection("jdbc:mysql://localhost:3306/blog","root","123456")
    insertStmt=conn.prepareStatement("INSERT INTO temperatures (sensor,temp) VALUES(?,?)")
    updateStmt=conn.prepareStatement("UPDATE temperatures SET temp= ? WHERE sensor = ?")
  }
//调用连接，执行sql
  override def invoke(value: SensorReading, context: SinkFunction.Context[_]): Unit = {
//执行更新语句
    updateStmt.setDouble(1,value.temperature)
    updateStmt.setString(2,value.id)
    updateStmt.execute()
    //如果update 没有查到数据那么执行插入语句
    if(updateStmt.getUpdateCount==0){
      insertStmt.setString(1,value.id)
      insertStmt.setDouble(2,value.temperature)
      insertStmt.execute()
    }
  }
  //释放资源
  override def close(): Unit = {
    insertStmt.close()
    updateStmt.close()
    conn.close()
  }
}