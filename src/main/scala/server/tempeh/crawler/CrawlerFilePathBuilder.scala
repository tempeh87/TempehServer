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
  val groupBy = 50

  def getTotalEpisodeSize: Int = {
    new File(dataParentFolderPath).listFiles().count(_.isDirectory)
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
        val episodeJson = Source.fromFile(s"${folder.getAbsolutePath}/$dataName").getLines().mkString(sep)
        val episode = new Gson().fromJson(episodeJson, classOf[Episode])
        episode
      })
      .grouped(groupBy)
      .zipWithIndex
      .foreach(groupWithIndex => {
        val (episodes: Array[Episode], index: Int) = groupWithIndex
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
    val fw = new FileWriter(fileToSave)
    fw.write(new Gson().toJson(episode))
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
      Some(new Gson().fromJson(Source.fromFile(fileName).getLines().mkString(sep), classOf[Episode]))
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