package org.jump.factory

import com.typesafe.scalalogging._
import org.slf4j.LoggerFactory

import org.ini4j.Ini
import java.io.FileReader
import scala.collection.JavaConversions._

import org.jump.parser._
import org.jump.entity._
import org.jump.db._
import org.jump.common._

object FieldFactory {
  val log = Logger(LoggerFactory.getLogger(this.getClass))

  def build(sectionTag: String): List[Field] = {
    var fields = ParameterParser.getFields(sectionTag)

    fields.map { x =>
      buildField(sectionTag, x)
    }.toList
  }

  def buildField(sectionTag: String, field: FieldConfig): Field = {
    var crawlerTypes = Set("one_of", "static", "serial", "sql")
    var supported = crawlerTypes ++ Set("fake")

    if (!supported.contains(field.getFnName)) {
      throw new RuntimeException(s"Unknown function [${field.getFnName}]")
    }

    if (crawlerTypes.contains(field.getFnName)) {
      new CrawlerField(field, buildCrawler(sectionTag, field))
    } else {
      if (field.getParams.size != 1) {
        throw new RuntimeException("The function [fake] takes only 1 parameter")
      }

      new FakerField(field)
    }
  }

  private def buildCrawler(sectionTag: String, field: FieldConfig): Crawler = {
    field.getFnName match {
      //TODO: ADD BETWEEN

      case "one_of" => buildOneOfCrawler(sectionTag, field)

      case "static" => buildStaticCrawler(sectionTag, field)

      case "serial" => buildSerialCrawler(sectionTag, field)

      case "sql"    => buildSqlCrawler(sectionTag, field)

      case _ => {
        throw new RuntimeException("Unknown function " + field.getFnName)
      }
    }
  }

  def isDigit(input: String): Boolean = {
    if (input.size == 0) {
      return false
    }

    val len = input.filter (x => Character.isDigit(x)).size

    if (len == input.size) {
      return true
    }

    return false
  }

  private def buildSqlCrawler(sectionTag: String, field: FieldConfig): Crawler = {
    val params = field.getParams.toList
    if (!Set(1, 2).contains(params.size)) {
      val msg1 = s"The method [sql] can be called with either 1 or 2 parameters: section [${sectionTag}]"
      val msg2 = "1. [sql(<sql_query>)] or 2. [sql(<sql_query>, int)]"
      throw new RuntimeException(msg1 + msg2)
    }

    var sql = params(0)
    var numInterations = 1

    if (params.size == 2) {
      if (isDigit(params(1))) {
        val iter = params(1).toInt
        if (iter > 0) {
          numInterations = iter
        }
      } else {
        throw new RuntimeException("Param 2 of the method sql should be an integer")
      }
    }

    val avList = DBManager.getAvList(sql, "av")

    new Crawler(avList, "serial", numInterations, sectionTag)
  }

  private def buildStaticCrawler(sectionTag: String, field: FieldConfig): Crawler = {
    if (field.getParams.size != 1) {
      throw new RuntimeException(s"The method static takes only 1 parameter: Error in section [${sectionTag}] Under the key fields")
    }

    new Crawler(field.getParams.toList, "serial", 1, sectionTag)
  }

  private def buildSerialCrawler(sectionTag: String, field: FieldConfig): Crawler = {
    new Crawler(field.getParams.toList, "serial", 1, sectionTag)
  }

  private def buildOneOfCrawler(sectionTag: String, field: FieldConfig): Crawler = {
    if (field.getParams.size < 1) {
      throw new RuntimeException(s"Not enough params for the function one_of in section [${sectionTag}] Under the key fields")
    }

    new Crawler(field.getParams.toList, "random", 1, sectionTag)
  }
}
