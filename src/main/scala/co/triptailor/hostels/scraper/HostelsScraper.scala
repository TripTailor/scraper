package co.triptailor.hostels.scraper

import java.io.BufferedReader
import co.triptailor.hostels.configuration.AppConfig
import java.io.FileReader
import scala.io.Source
import scala.collection.JavaConverters._
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.File
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import sys.process._
import co.triptailor.hostels.configuration.HostelOffset
import co.triptailor.hostels.configuration.CityLineWithIndex
import co.triptailor.hostels.configuration.LocationInfo
import co.triptailor.hostels.configuration.HostelUrlWithIndex
import co.triptailor.hostels.configuration.Review
import co.triptailor.hostels.configuration.ReviewWithIndex


/**
 * @author lgaleana
 */
object HostelsScraper extends Scraper {
  
  def main(args: Array[String]) = {
    scrape()
  }

  def scrape(): Unit = {
    val offset = Source.fromFile(AppConfig.Data.lastHostel).getLines.next.split(",") match {
      case Array(a, b, c) => HostelOffset(a.toInt, b.toInt, c.toInt)
    }
    
    val citiesWithIndex = Source.fromFile(AppConfig.Data.citiesFile).getLines.toSeq.drop(offset.city).zipWithIndex.map(_ match {
      case (cityLine, index) => CityLineWithIndex(cityLine, index)
    })
    for (cityWithIndex <- citiesWithIndex) {
      val cityInfo = cityWithIndex.cityLine.split(",") match {
        case Array(city, state, country, continent, url) => LocationInfo(country, city, url)
      }
      
      val hostelOffset = if(cityWithIndex.index == 0) offset.hostel else 0
      val reviewOffset = if(cityWithIndex.index == 0) offset.review else 0
      try {
        scrapeCityUntil(cityInfo, if(hostelOffset < AppConfig.JSoup.hostelsOffset) -1 else getFirstHostelWorldId(cityInfo.url),
            HostelOffset(offset.city + cityWithIndex.index, hostelOffset, reviewOffset))
        saveLast(HostelOffset(offset.city + cityWithIndex.index + 1, 0, 0))
      } catch {
        case ex: java.net.SocketTimeoutException => {
          System.err.println("Read timed out")
          Thread.sleep(AppConfig.General.sleepTime)
          scrape()
        }
      }
    }
  }
  
  def scrapeCityUntil(cityInfo: LocationInfo, firstId: Int, offset: HostelOffset): Unit = {
    val page = offset.hostel / AppConfig.JSoup.hostelsOffset + 1
    val url = cityInfo.url + "?page=" + page
    println("Scraping " + url)
    val doc = getDocument(url)
    
    val hostelBoxes = doc.select(".fabdetails").asScala
    val hostelUrls = hostelBoxes.map(_.select("a").get(0).attr("href").split("\\?")(0))
    
    if(hostelUrls.size > 0) {
      val currentFirstId = getHostelWorldId(hostelUrls(0))
      
      if(currentFirstId != firstId) {
        val hostelUrlsWithIndex = hostelUrls.drop(offset.hostel % AppConfig.JSoup.hostelsOffset).zipWithIndex.map(_ match {
          case (hostelUrl, index) => HostelUrlWithIndex(hostelUrl, index)
        })
        for(hostelUrlWithIndex <- hostelUrlsWithIndex) {
          val reviewOffset = if(hostelUrlWithIndex.index == 0) offset.review else 0
          scrapeHostel(hostelUrlWithIndex.url, HostelOffset(offset.city, offset.hostel + hostelUrlWithIndex.index, reviewOffset),
              cityInfo)
        }
        
        val firstHostelWorldId = if(firstId != -1) firstId else currentFirstId
        scrapeCityUntil(cityInfo, firstHostelWorldId, HostelOffset(offset.city, page * AppConfig.JSoup.hostelsOffset, 0))
      }
    }
  }
  
  def scrapeHostel(url: String, offset: HostelOffset, cityInfo: LocationInfo) = {
    val path = createDirectoriesIfNotExist(cityInfo.country, cityInfo.city)
      
    val hostelPath = saveHostelInformation(url, path)
    
    if(AppConfig.General.scrapeReviews) {
      val reviewsWriter = new BufferedWriter(new FileWriter(hostelPath + "/reviews.txt", true))
      scrapeReviewsUntil(url + "/reviews/{offset}?period=all", offset, reviewsWriter)
      reviewsWriter.close
    }
    
    saveLast(HostelOffset(offset.city, offset.hostel + 1, 0))
  }
  
  def saveHostelInformation(url: String, path: String) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    
    val name = doc.select(".main").get(0).select("h1").get(0).text
    
    val hostelDir = new File(path + "/" + name.replaceAll("[/ ]", "_"))
    if(!hostelDir.exists())
      hostelDir.mkdir
    
    val writer = new BufferedWriter(new FileWriter(hostelDir.getAbsolutePath + "/info.txt"))
    
    val hostelWorldId = getHostelWorldId(url)
    writer.write(hostelWorldId + "\n")
    
    writer.write(name + "\n")
    writer.write(url + "\n")
    
    val address = doc.select(".address").text
    writer.write(address + "\n")
    
    val latitude = doc.select("span[itemprop=\"latitude\"]").text
    val longitude = doc.select("span[itemprop=\"longitude\"]").text
    writer.write(latitude + "," + longitude + "\n")
    
    val description = "<p>" + doc.select(".microdetailstext.prop-text").html
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
    
    saveImagesUrls(doc, hostelDir.getAbsolutePath)
    
    hostelDir.getAbsolutePath
  }
  
  def scrapeReviewsUntil(reviewsUrl: String, offset: HostelOffset, writer: BufferedWriter): Unit = {
    val page = offset.review / AppConfig.JSoup.reviewsOffset + 1
    val url = reviewsUrl.replace("{offset}", page.toString)
    println("Scraping " +  url)
    val doc = getDocument(url)
    
    val reviews = doc.select(".reviewBox").asScala.drop(offset.review % AppConfig.JSoup.reviewsOffset)
    val reviewsWithIndex= reviews.map(matchReview).zipWithIndex.map(_ match {
      case (review, index) => ReviewWithIndex(review, index)
    })
    
    if(reviewsWithIndex.size > 0) {
      for(reviewWithIndex <- reviewsWithIndex) {
        val date = reviewWithIndex.review.meta.split(",")(0).split(" ")
        val year = if(date.size > 2) date(2).toInt else 0
        if(year <= 2015) {
          writer.write(reviewWithIndex.review.meta + "\n" + reviewWithIndex.review.text + "\n")
          writer.flush
        }
        saveLast(HostelOffset(offset.city, offset.hostel, reviewWithIndex.index + offset.review + 1))
      }
      
      scrapeReviewsUntil(reviewsUrl, HostelOffset(offset.city, offset.hostel, page * AppConfig.JSoup.reviewsOffset), writer)
    }
  }
  
  def saveImagesUrls(doc: Document, path: String) = {
    if(AppConfig.General.downloadImages)
      print("Saving images ")
    val writer = new BufferedWriter(new FileWriter(path + "/images.txt"))
    val imagesWrapper = doc.getElementById("gallery-imagelist")
    if(imagesWrapper != null) {
      val imagesUrls = imagesWrapper.select("a").asScala.map(_.attr("href"))
      imagesUrls.foreach(imageUrl => {
        writer.write(imageUrl + "\n")
        if(AppConfig.General.downloadImages) {
          val imagesDir = new File(path + "/images")
          if(!imagesDir.exists())
            imagesDir.mkdir
          val uris = imageUrl.split("/")
          val name = uris(uris.length - 1)
          print(".")
          try {
            new URL(imageUrl) #> new File(imagesDir.getAbsolutePath + "/" + name) !!
          } catch {
            case e: Exception =>
          }
        }
      })
    }
    writer.close
    if(AppConfig.General.downloadImages)
      println
  }
  
  def getHostelWorldId(url: String) = {
    val uris = url.split("/")
    uris(uris.size - 1).toInt
  }
  
  def getFirstHostelWorldId(url: String) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    
    val hostelBoxes = doc.select(".fabdetails").asScala
    val hostelUrls = hostelBoxes.map(_.select("a").get(0).attr("href").split("\\?")(0))
    getHostelWorldId(hostelUrls(0))
  }
  
  def matchReview(review: Element) = {
    Review(review.child(0).text, review.child(1).select("li").asScala.filter(element => {
      val possibleAnchors = element.select("a")
      possibleAnchors.size == 0 || !possibleAnchors.get(0).attr("class").equals("travelertype")
    }).map(_.text).toSeq match {
      case Seq(date, user, country, sexAge, reviews) => date + "," + user + "," + country + "," +
        sexAge.replace(" ", "") + "," + reviews.replaceAll("[\\((?: reviews\\))]", "")
      case Seq(date, user, country, reviews) => date + "," + user + "," + country + ",,," + reviews
      case Seq(date, user, country) => date + "," + user + "," + country + ",,,"
      case Seq(date, user) => date + "," + user + ",,,,"
      case Seq(date) => date + ",,,,,"
      case Seq() => ",,,,,"
    })
  }
  
  def saveLast(offset: HostelOffset) = {
    val lastWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastHostel))
    lastWriter.write(offset.city + "," + offset.hostel + "," + offset.review)
    lastWriter.close
  }
  
  def createDirectoriesIfNotExist(country: String, city: String) = {
    val countryDir = new File(AppConfig.Data.data + country.replaceAll("[/ ]", "_"))
    if(!countryDir.exists())
      countryDir.mkdir
    val cityDir = new File(countryDir.getAbsolutePath + "/" + city.replaceAll("[/ ]", "_"))
    if(!cityDir.exists())
      cityDir.mkdir
      
    cityDir.getAbsolutePath
  }
  
}