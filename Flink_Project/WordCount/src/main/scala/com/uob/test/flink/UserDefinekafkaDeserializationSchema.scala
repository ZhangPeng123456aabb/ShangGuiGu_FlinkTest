package com.uob.test.flink

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema
import org.apache.flink.util.StringUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.flink.streaming.api.scala._
/**
  * @author ZhangPeng
  * @Email ZhangPeng1853093@126.com
  * @date 2020/3/18 - 16:45 
  */
class UserDefinekafkaDeserializationSchema extends KafkaDeserializationSchema[(Int,Long,String,String,String)] {
  override def isEndOfStream(t: (Int, Long, String, String, String)): Boolean = {
    false
  }

  override def deserialize(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): (Int, Long, String, String, String) = {
    if(consumerRecord.key()==null){
      (consumerRecord.partition(),consumerRecord.offset(),consumerRecord.topic(),"",new String(consumerRecord.value()))
    }else{
      (consumerRecord.partition(),consumerRecord.offset(),consumerRecord.topic(),StringUtils.arrayToString(consumerRecord.key()),new String(consumerRecord.value()))
    }
  }
  //告知返回值类型
  override def getProducedType: TypeInformation[(Int, Long, String, String, String)] = {
  createTypeInformation[(Int,Long,String,String,String)]
  }
}
