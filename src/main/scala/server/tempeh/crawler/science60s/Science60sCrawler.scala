package server.tempeh.crawler.science60s

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
    val before = pathBuilder.getTotalEpisodeSize
    crawl()
    val after = pathBuilder.getTotalEpisodeSize
    val changed = (after - before) > 0
    if (changed) {
      buildIndex()
    }
    changed
  }

  def crawl(): Unit = {
    val lastFind = pathBuilder.findLastUrl
    val startIndex = findStartIndex(1, lastFind)
    (startIndex to 1 by -1).toStream.foreach(index => {
      extractEpisodeIndex(buildLinkWithIndex(index)).toStream.takeWhile(_ != lastFind).reverse.foreach(link => {
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

  def findStartIndex(currentIndex: Int, crawlUntil: String): Int = {
    val url = buildLinkWithIndex(currentIndex)
    val links = extractEpisodeIndex(url)
    if (links.contains(crawlUntil) || links.isEmpty) {
      currentIndex
    } else {
      findStartIndex(currentIndex + 1, crawlUntil)
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
    * */
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
