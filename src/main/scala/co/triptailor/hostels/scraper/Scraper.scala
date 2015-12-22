package co.triptailor.hostels.scraper

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import co.triptailor.hostels.configuration.AppConfig
import org.jsoup.HttpStatusException

/**
 * @author lgaleana
 */
trait Scraper {
  def getDocument(url: String) = {
    try {
      Jsoup.connect(url)
      .userAgent(AppConfig.Headers.userAgent)
      .header("Accept", AppConfig.Headers.accept)
      .header("Accept-Charset", AppConfig.Headers.acceptCharset)
      .header("Accept-Encoding", AppConfig.Headers.acceptEncoding)
      .header("Accept-Language", AppConfig.Headers.acceptLanguage)
      .get
    } catch {
      case e: HttpStatusException => new Document("/")
    }
  }
  
  def getTripAdvisorId(anchor: Element, split: String) = {
    anchor.attr("href").split(split)(1).split("-")(0).toInt
  }
}