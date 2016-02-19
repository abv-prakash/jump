package org.jump.common

import com.typesafe.scalalogging._
import org.slf4j.LoggerFactory

import org.ini4j.Ini
import java.io.FileReader
import scala.collection.JavaConversions._

import org.jump.entity._
import org.jump.manager._

object SqlBuilder {
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  def buildInsert(sectionTag: String, fields: List[Field]): List[String] = {
    val numRows = IniManager.getKey(sectionTag, "rows").toInt
    val batchSize = AppConfig.conf.getInt("batch_size")

    if (numRows < 1) {
      throw new RuntimeException("[rows] should be atleast 1")
    }

    val totalIterations = numRows / batchSize
    val remaining = numRows % batchSize

    val tableName = IniManager.getKey(sectionTag, "table")

    val builder = new java.util.ArrayList[String]
    for (i <- 0 until totalIterations) {
      builder.add(buildBatch(fields, batchSize, tableName))
    }

    if (remaining > 0) {
      builder.add(buildBatch(fields, remaining, tableName))
    }

    builder.toList
  }

  private def buildBatch(fields: List[Field], rows: Int, tableName: String): String = {
    val container = new java.util.ArrayList[String]

    for (i <- 0 until rows) {
      container.add(buildRow(fields))
    }

    var finalSql = "insert into " + tableName
    finalSql += "(" + fields.map (x => x.getName).toList.mkString(",") + ") values "
    finalSql += container.toList.mkString(",")

    finalSql
  }

  private def buildRow(fields: List[Field]): String = {
    val builder = new StringBuilder
    var prefix = ""

    builder.append("(")

    fields.map { x =>
      builder.append(prefix)
      builder.append(x.produce)
      prefix = ","
    }

    builder.append(")")
    builder.toString
  }
}
