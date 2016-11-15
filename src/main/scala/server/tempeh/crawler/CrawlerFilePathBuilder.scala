package server.tempeh.crawler

import java.io.{File, FileWriter}

import com.google.gson.Gson
import org.apache.commons.io.FileUtils
import server.tempeh.category.DataSource
import server.tempeh.model.Episode

import scala.io.Source

/**
  * To present the specific folder structure for persisting data and config.
  * With given root path and data source, the crawler will generate folders below:
  * - data source name
  * - data
  * - time_stamp/data1
  * - time_stamp/data2
  * - ...
  * - config.txt
  **/
class CrawlerFilePathBuilder(val rootPath: String, val dataSource: DataSource) {
  private val dataSourcePath: String = s"$rootPath/${dataSource.name()}"
  val configFilePath: String = s"$dataSourcePath/config.txt"
  val dataParentFolderPath: String = s"$dataSourcePath/data"
  val indexParentFolderPath: String = s"$dataSourcePath/index"
  val sep = "\r\n"
  val dataName = "episode.txt"
  val groupBy = 50 //magic!

  def getTotalEpisodeSize: Int = {
    val dataParent = new File(dataParentFolderPath)
    if(!dataParent.exists()){
      dataParent.mkdirs()
    }
    dataParent.listFiles().count(_.isDirectory)
  }

  def clearIndexFolder: Unit = {
    FileUtils.deleteDirectory(new File(indexParentFolderPath))
  }

  def rebuildIndex: Unit = {
    new File(dataParentFolderPath).listFiles()
      .filter(_.isDirectory)
      .sortWith((left, right) => {
        left.getName.toLong > right.getName.toLong
      })
      .map(folder => {
        val episodeEncrypted = Source.fromFile(s"${folder.getAbsolutePath}/$dataName").mkString
        episodeEncrypted
      })
      .grouped(groupBy)
      .zipWithIndex
      .foreach(groupWithIndex => {
        val (episodes: Array[String], index: Int) = groupWithIndex
        val fileToWrite = new File(s"$indexParentFolderPath/$index.txt")
        if (!fileToWrite.getParentFile.exists()) {
          fileToWrite.getParentFile.mkdirs()
        }
        val fw = new FileWriter(fileToWrite)
        fw.write(new Gson().toJson(episodes))
        fw.flush()
        fw.close()
      })
  }

  def saveEpisode(episode: Episode): Unit = {
    println(s"persisting.. ${episode.title}")
    val fileToSave = new File(s"$dataParentFolderPath/${System.currentTimeMillis()}/$dataName")
    fileToSave.getParentFile.mkdirs()
    println(s"save to.. ${fileToSave.getAbsolutePath}")
    val fw = new FileWriter(fileToSave)
    fw.write(Episode.encrypt(episode))
    fw.flush()
    fw.close()
  }

  def findLastUpdated(): Option[Episode] = {
    new File(dataParentFolderPath).mkdirs()
    val folders = new File(dataParentFolderPath).listFiles()
    if (folders.isEmpty) {
      None
    } else {
      val latest = folders.filter(_.isDirectory).sortWith((left, right) => {
        left.getName.toLong > right.getName.toLong
      }).head
      val fileName = s"${latest.getAbsolutePath}/$dataName"
      Some(Episode.decrypt(Source.fromFile(fileName).mkString))
    }
  }

  def findLastUrl: String = {
    val last = findLastUpdated()
    if (last.isDefined) {
      last.get.url
    } else {
      ""
    }
  }
}
