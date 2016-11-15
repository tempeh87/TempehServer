package server.tempeh.crawler

import java.io.File

import server.tempeh.crawler.science60s.Science60sCrawler

object Main {
  def main(args: Array[String]) {
    val currentPath = new File(getClass.getResource(".").toURI).getAbsolutePath
    val resource = currentPath.split("/target/").head
    val staticFileRoot = s"$resource/src/main/resources/static"
    val science60sCrawler = new Science60sCrawler(staticFileRoot)
    if (science60sCrawler.start()) {
      //update
      System.exit(0)
    } else {
      //failed
      System.exit(1)
    }
  }
}
