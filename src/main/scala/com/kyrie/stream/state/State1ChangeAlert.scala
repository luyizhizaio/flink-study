package com.kyrie.stream.state

import com.kyrie.stream.watermark.Feedback2
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector
import sun.plugin2.jvm.CircularByteBuffer.Streamer

/**
 * 两次变化超过10发出报警
 */
object State1ChangeAlert {

  def main(args: Array[String]): Unit = {


    val  env = StreamExecutionEnvironment.getExecutionEnvironment


    val stream = env.socketTextStream("localhost",9999)


    val keyStream = stream.map{line =>
      val Array(id,timestamp,fre) = line.split(" ")
      Feedback2(id,timestamp.toLong, fre.toInt)
    }.keyBy(_.id)

    keyStream.print("key:")

    //定义flatMap类，并执行
    //val flatStream = keyStream.flatMap(new TemperatureAlertFunction(10))

    keyStream.flatMapWithState()

    flatStream.print("flat:")

    env.execute()

  }

}

class TemperatureAlertFunction(threshold:Int) extends RichFlatMapFunction[Feedback2,(String,Int,Int)]{

  private var lastTempState:ValueState[Int] =_

  override def open(parameters: Configuration): Unit = {

    val descriptor = new ValueStateDescriptor[Int]("lastTemp",classOf[Int])

    lastTempState = getRuntimeContext.getState[Int](descriptor)


  }

  override def flatMap(value: Feedback2, out: Collector[(String, Int, Int)]): Unit = {

    val lastTemp = lastTempState.value()

    val diff = (value.fre - lastTemp).abs

    if(diff > threshold){
      out.collect((value.id, lastTemp,value.fre))
    }else{
      this.lastTempState.update(value.fre)
    }
  }
}




