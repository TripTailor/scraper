package co.triptailor.hostels

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * @author lgaleana
 */
trait Scraper {
  def getDocument(url: String): Document = {
    Jsoup.connect(url)
                       .userAgent(AppConfig.Headers.userAgent)
                       .header("Accept", AppConfig.Headers.accept)
                       .header("Accept-Charset", AppConfig.Headers.acceptCharset)
                       .header("Accept-Encoding", AppConfig.Headers.acceptEncoding)
                       .header("Accept-Language", AppConfig.Headers.acceptLanguage)
                       .get()
  }
  
  def getTripAdvisorId(anchor: Element, split: String): Int = {
    anchor.attr("href").split(split)(1).split("-")(0).toInt
  }
}