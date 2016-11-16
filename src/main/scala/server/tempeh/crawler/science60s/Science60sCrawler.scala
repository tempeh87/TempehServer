package server.tempeh.crawler.science60s

import java.io.File

import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import server.tempeh.category.DataSource
import server.tempeh.crawler.CrawlerFilePathBuilder
import server.tempeh.model.Episode

import scala.collection.JavaConverters._

class Science60sCrawler(val root: String) {
  private val timeout = 21000
  private val dataSource: DataSource = DataSource.Science60s
  val pathBuilder: CrawlerFilePathBuilder = new CrawlerFilePathBuilder(root, DataSource.Science60s)

  def start(): Boolean = {
    val before = pathBuilder.findLatestFolderName
    crawl()
    val after = pathBuilder.findLatestFolderName
    val changed = !after.equals(before)
    if (changed) {
      buildIndex()
    }
    changed
  }

  def crawl(): Unit = {
    val lastFind = pathBuilder.findLastEpisodes
    val startIndex = findStartIndexAndRemoveNonExist(1, lastFind)
    println(s"start index: $startIndex")
    val modifiedStopper = pathBuilder.findLastEpisode
    (startIndex to 1 by -1).toStream.foreach(index => {
      extractEpisodeIndex(buildLinkWithIndex(index)).toStream.takeWhile(_ != modifiedStopper).reverse.foreach(link => {
        val episode = extractHyperLinkToEpisode(link)
        pathBuilder.saveEpisode(episode)
      })
    })
  }

  def buildIndex() = {
    pathBuilder.clearIndexFolder
    pathBuilder.rebuildIndex
  }

  def getEpisodeCount: Int = {
    pathBuilder.getTotalEpisodeSize
  }

  def findStartIndexAndRemoveNonExist(currentIndex: Int, crawlUntil: List[(Episode, File)]): Int = {
    val url = buildLinkWithIndex(currentIndex)
    println(s"trying $url")
    val links = extractEpisodeIndex(url)
    val find: Option[((Episode, File), Int)] = crawlUntil.zipWithIndex.find(ef => {
      val episode: Episode = ef._1._1
      links.contains(episode.url)
    })

    find match {
      case Some(obj) =>
        val ((episode, _), index) = obj
        val (remove, _) = crawlUntil.sortWith((l,r)=>{
          val episodeFileL = l._2.getParentFile.getName.toDouble
          val episodeFileR = r._2.getParentFile.getName.toDouble
          episodeFileL > episodeFileR
        }).splitAt(index)
        if (remove.nonEmpty) {
          remove.foreach(ef => {
            val (_, f) = ef
            FileUtils.deleteDirectory(f.getParentFile)
            println(s"remove ${episode.title}")
          })
        }
        currentIndex
      case None =>
        findStartIndexAndRemoveNonExist(currentIndex + 1, crawlUntil)
    }
  }

  def buildLinkWithIndex(index: Int): String = {
    s"https://www.scientificamerican.com/podcast/60-second-science/?page=$index"
  }

  /**
    * @param url https://www.scientificamerican.com/podcast/60-second-science/?page=1
    * @return List of string for extractHyperLinkToEpisode to parse more info
    **/
  def extractEpisodeIndex(url: String): List[String] = {
    Jsoup.connect(url).timeout(timeout).get().select("h3 a").asScala.toStream.dropRight(2).map(element => {
      element.absUrl("href").trim
    }).toList
  }

  /** *
    *
    * @param url : https://www.scientificamerican.com/podcast/episode/orangutan-picks-cocktail-by-seeing-ingredients/
    **/
  def extractHyperLinkToEpisode(url: String): Episode = {
    Thread.sleep(1000)
    val document = Jsoup.connect(url).timeout(timeout).get()
    val title = document.select("h3").first().text().trim
    val transcript = document.select("div.transcript__inner p").asScala.map(_.text()).mkString("\r\n").replaceAll("\\[.*?\\]", "").trim
    val audio = document.select("audio source").first().absUrl("src")
    println(s"parsing.. $url")
    Episode(title, transcript = transcript, audioSource = audio, url = url, dataSource = this.dataSource)
  }
}
