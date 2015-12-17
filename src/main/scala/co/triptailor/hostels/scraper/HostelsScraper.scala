package co.triptailor.hostels.scraper

import java.io.BufferedReader
import co.triptailor.hostels.configuration.AppConfig
import java.io.FileReader
import scala.io.Source
import scala.collection.JavaConverters._
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.File


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
      val cityInfo = lineIndex._1.split(",") match {
        case Array(city, state, country, continent, url) => (country, city, url)
      }
      
      val hostelOffset = if(lineIndex._2 == 0) offset._2 else 0
      val reviewOffset = if(lineIndex._2 == 0) offset._3 else 0
      scrapeCityUntil(cityInfo, -1, (offset._1 + lineIndex._2, hostelOffset, reviewOffset))
    }
  }
  
  def scrapeCityUntil(cityInfo: (String, String, String), firstId: Int, offset: (Int, Int, Int)): Unit = {
    val url = cityInfo._3 + "?page=" + offset._2
    println("Scraping " + url)
    val doc = getDocument(url)
    val hostelBoxes = doc.select(".fabdetails").asScala
    val hostelUrls = hostelBoxes.map(_.select("a").get(0).attr("href").split("\\?")(0))
    
    val currentFirstId = getHostelWorldId(hostelUrls(0))
    
    if(currentFirstId != firstId) {
      val urlsZip = hostelUrls.drop(offset._2).zipWithIndex
      for(urlIndex <- urlsZip) {
        val reviewOffset = if(urlIndex._2 == 0) offset._3 else 0
        scrapeHostel(urlIndex._1, (offset._1, offset._2 + urlIndex._2, reviewOffset), cityInfo)
      }
      
      val firstHostelWorldId = if(firstId != -1) firstId else currentFirstId
      scrapeCityUntil(cityInfo, firstHostelWorldId, (offset._1, offset._2 + 1, 0))
    }
  }
  
  def scrapeHostel(url: String, offset: (Int, Int, Int), cityInfo: (String, String, String)) = {
    val path = createDirectoriesIfNotExist(cityInfo._1, cityInfo._2)
      
    saveHostelInformation(url, path)
    scrapeReviewsUntil(url + "/reviews?reviewsLanguage=all&period=all", offset, path)
  }
  
  def saveHostelInformation(url: String, path: String) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    
    val name = doc.select(".main").get(0).select("h1").get(0).text
    
    val hostelDir = new File(AppConfig.Data.data + name.replaceAll(" ", "_"))
    if(!hostelDir.exists())
      hostelDir.mkdir
    
    val writer = new BufferedWriter(new FileWriter(path + "/" + name))
    
    val hostelWorldId = getHostelWorldId(url)
    writer.write(hostelWorldId + "\n")
    
    writer.write(name + "\n")
    
    val description = "<p>" + doc.select(".microdetailstext.prop-text.bigtext").html
      .replaceAll("[\r\n]", "").replaceAll(" <br> <br> ", "</p><p>") + "</p>"
    writer.write(description + "\n")
    
    val details = doc.select(".bottondetails-left").get(0)
    
    val facilitiesPolicies = details.select(".facilities").asScala
    val facilities = if(facilitiesPolicies.size > 0) facilitiesPolicies(0).select("li").asScala.map(_.text).toSeq
      else Seq()
    val policies = if(facilitiesPolicies.size > 1) facilitiesPolicies(1).select("li").asScala.map(_.text).toSeq
      else Seq()
    writer.write(facilities.size + "\n")
    facilities.foreach(facility => writer.write(facility + "\n"))
    writer.write(policies.size + "\n")
    policies.foreach(policy => writer.write(policy + "\n"))
    
    val cancellationWords = details.select("strong").get(0).text.split(" ")
    val cancellationIndex = cancellationWords.indexWhere(_.forall(_.isDigit))
    val cancellation = if(cancellationIndex != -1)
      cancellationWords(cancellationIndex) + " " + cancellationWords(cancellationIndex + 1) else ""
    writer.write(cancellation)
    
    writer.close
  }
  
  def scrapeReviewsUntil(url: String, offset: (Int, Int, Int), path: String): Unit = {
    
  }
  
  def getHostelWorldId(url: String) = {
    val uris = url.split("/")
    uris(uris.size - 1).toInt
  }
  
  def createDirectoriesIfNotExist(country: String, city: String) = {
    val countryDir = new File(AppConfig.Data.data + country.replaceAll(" ", "_"))
    if(!countryDir.exists())
      countryDir.mkdir
    val cityDir = new File(countryDir.getAbsolutePath + "/" + city.replaceAll(" ", "_"))
    if(!cityDir.exists())
      cityDir.mkdir
      
    cityDir.getAbsolutePath
  }
  
}