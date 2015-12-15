package co.triptailor.hostels.scraper

import java.io.BufferedReader
import co.triptailor.hostels.configuration.AppConfig
import java.io.FileReader
import scala.io.Source
import scala.collection.JavaConverters._


/**
 * @author lgaleana
 */
object HostelsScraper extends Scraper {
  
  def main(args: Array[String]) = {
    scrape()
  }

  def scrape(): Unit = {
    val offset = Source.fromFile(AppConfig.Data.lastHostel).getLines.next.split(",") match {
      case Array(a, b, c) => (a.toInt, b.toInt, c.toInt)
    }
    
    val linesZip = Source.fromFile(AppConfig.Data.citiesFile).getLines.toSeq.drop(offset._1).zipWithIndex
    for (lineIndex <- linesZip) {
      val cityUrl = lineIndex._1.split(",") match {
        case Array(city, state, country, continent, url) => url
      }
      
      val hostelOffset = if(lineIndex._2 == 0) offset._2 else 0
      val reviewOffset = if(lineIndex._2 == 0) offset._3 else 0
      scrapeCityUntil(cityUrl, -1, (offset._1 + lineIndex._2, hostelOffset, reviewOffset))
    }
  }
  
  def scrapeCityUntil(cityUrl: String, firstId: Int, offset: (Int, Int, Int)): Unit = {
    val url = cityUrl + "?page=" + offset._2
    println("Scraping " + url)
    val doc = getDocument(url)
    val hostelBoxes = doc.select(".fabdetails").asScala
    val hostelUrls = hostelBoxes.map(_.select("a").get(0).attr("href").split("?")(0))
    
    val currentFirstId = getHostelWorldId(hostelUrls(0))
    
    if(currentFirstId != firstId) {
      val urlsZip = hostelUrls.drop(offset._2).zipWithIndex
      for(urlIndex <- urlsZip) {
        val reviewOffset = if(urlIndex._2 == 0) offset._3 else 0
        scrapeHostel(urlIndex._1, (offset._1, offset._2 + urlIndex._2, reviewOffset))
      }
      
      val firstHostelWorldId = if(firstId != -1) firstId else currentFirstId
      scrapeCityUntil(cityUrl, firstHostelWorldId, (offset._1, offset._2 + 1, 0))
    }
  }
  
  def scrapeHostel(url: String, offset: (Int, Int, Int)) = {
    
  }
  
  def getHostelWorldId(url: String) = {
    val uris = url.split("/")
    uris(uris.size - 1).toInt
  }
  
}