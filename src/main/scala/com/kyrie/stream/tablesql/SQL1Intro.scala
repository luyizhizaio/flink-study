package com.kyrie.stream.tablesql

import com.kyrie.stream.watermark.Feedback2
import org.apache.flink.table.api._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.scala._
import org.apache.flink.api.scala._

object SQL1Intro {
  """
    |Table API 是流处理和批处理通用的关系型 API，
    |Table API 可以基于流输入或者批输入来运行而不需要进行任何修改。
    |""".stripMargin


  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment


    val stream = env.socketTextStream("192.168.0.104",9999,'\n')


    val tableEnv = TableEnvironment.getTableEnvironment(env)

    val feedbackStream = stream.map{line =>
      val Array(id,ts,num) = line.split(",")
      Feedback2(id,ts.toLong,num.toInt)
    }

    //stream转table
    val table :Table= tableEnv.fromDataStream(feedbackStream)

    val tab2 = table.select("id,fre").filter("id = 'id1'")

    tab2.printSchema()

    //table 转stream
    val resultStream = tableEnv.toAppendStream[(String,Int)](tab2)

    resultStream.print("result")


    /*********************************************/

    val stream2:DataStream[(String,Long,Int)]= stream.map{line =>
      val Array(id,ts,num) = line.split(",")
      (id,ts.toLong,num.toInt)
    }

    //转换元祖为table，指定字段名称
   val tab3 = tableEnv.fromDataStream(stream2, 'id2, 'ts, 'fre)

    tab3.select("id,fre").filter("id = 'id1'")

    env.execute()

  }


}
