package server.tempeh.crawler

import server.tempeh.crawler.science60s.Science60sCrawler

object Main {
  def main(args: Array[String]) {
    val science60sCrawler = new Science60sCrawler(args(0))
    science60sCrawler.start()
  }
}
