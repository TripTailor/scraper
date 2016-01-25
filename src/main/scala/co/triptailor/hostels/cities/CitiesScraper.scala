package co.triptailor.hostels.cities

import java.io.FileWriter
import java.io.BufferedWriter
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import java.io.BufferedReader
import java.io.FileReader
import co.triptailor.hostels.configuration.AppConfig
import co.triptailor.hostels.scraper.Scraper
import scala.io.Source
import co.triptailor.hostels.configuration.LocationOffset
import co.triptailor.hostels.configuration.CountryWithIndex
import co.triptailor.hostels.configuration.StateWithIndex
import co.triptailor.hostels.configuration.ContinentWithIndex
import co.triptailor.hostels.configuration.CityWithIndex

/**
 * @author lgaleana
 */
object CitiesScraper extends Scraper {
  def main(args: Array[String]) = {
    scrape(AppConfig.JSoup.citiesUrl)
  }
  
  def scrape(url: String): Unit = {
    val offset = Source.fromFile(AppConfig.Data.lastHostel).getLines.next.split(",") match {
      case Array(continent, country, state, city) =>
        LocationOffset(continent.toInt, country.toInt, state.toInt, city.toInt)
    }
    
    try {
      scrapeCities(AppConfig.JSoup.citiesUrl, offset)
    } catch {
      case ex: java.net.SocketTimeoutException => {
        System.err.println(ex.getStackTrace)
        Thread.sleep(AppConfig.General.sleepTime)
        scrape(url)
      }
    }
  }
  
  def scrapeCities(url: String, offset: LocationOffset) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    
    val countriesContainer = doc.select(".toprated.rounded").get(0)
    val continentsWithIndex = countriesContainer.children().asScala.drop(offset.continent).sliding(2, 2).map{child =>
      (child(0).text, child(1).select("a").asScala.toSeq)
    }.toSeq.zipWithIndex match {
      case Seq(((continent, countries), index)) => Seq(ContinentWithIndex(continent, countries, index))
    }
    
    val writer = new BufferedWriter(new FileWriter(AppConfig.Data.citiesFile, true))
    for(continentWithIndex <- continentsWithIndex) {
      val countryOffset = if(continentWithIndex.index == 0) offset.country else 0
      val stateOffset = if(continentWithIndex.index == 0) offset.state else 0
      val cityOffset = if(continentWithIndex.index == 0) offset.city else 0
      
      scrapeContinent(continentWithIndex.continent, continentWithIndex.countryElements, writer,
          LocationOffset(offset.continent + continentWithIndex.index, countryOffset, stateOffset, cityOffset))
      
      saveLast(LocationOffset(offset.continent + continentWithIndex.index + 1, 0, 0, 0))
    }
    writer.close
  }
  
  def scrapeContinent(continent: String, countryElements: Seq[Element], writer: BufferedWriter, offset: LocationOffset) = {
    val countriesWithIndex = countryElements.drop(offset.country).zipWithIndex match {
      case Seq((countryElement, index)) => Seq(CountryWithIndex(countryElement, index))
    }
    
    for(countryWithIndex <- countriesWithIndex) {
      val countryUrl = countryWithIndex.countryElement.attr("href")
      val country = countryWithIndex.countryElement.text
      
      val stateOffset = if(countryWithIndex.index == 0) offset.state else 0
      val cityOffset = if(countryWithIndex.index == 0) offset.city else 0
      
      scrapeCountry(countryUrl, country, continent, writer,
          LocationOffset(offset.continent, offset.country + countryWithIndex.index, stateOffset, cityOffset))
      
      saveLast(LocationOffset(offset.continent, offset.country + countryWithIndex.index + 1, 0, 0))
    }
  }
  
  def scrapeCountry(url: String, country: String, continent: String, writer: BufferedWriter, offset: LocationOffset) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    val cities = doc.getElementById("bottomlist").select("a").asScala
    if(!cities.isEmpty) {
      saveCities(cities, "", country, continent, writer, LocationOffset(offset.continent, offset.country, 0, offset.city))
      
      saveLast(LocationOffset(offset.continent, offset.country, 0, 0))
    }
    else {
      val statesWithIndex = doc.getElementById("states").select("a").asScala.drop(offset.state).zipWithIndex match {
        case Seq((stateElement, index)) => Seq(StateWithIndex(stateElement, index))
      }
      
      for(stateWithIndex <- statesWithIndex) {
        val state = stateWithIndex.stateElement.text
        val cities = scrapeStateForCities(stateWithIndex.stateElement.attr("href"))
        
        val cityOffset = if(stateWithIndex.index == 0) offset.city else 0
        
        saveCities(cities, state, country, continent, writer,
            LocationOffset(offset.continent, offset.country, offset.state + stateWithIndex.index, cityOffset))
        
        saveLast(LocationOffset(offset.continent, offset.country, offset.state + stateWithIndex.index + 1, 0))
      }
    }
  }
  
  def scrapeStateForCities(url: String) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    doc.getElementById("bottomlist").select("a").asScala
  }
  
  def saveCities(cities: Seq[Element], state: String, country: String, continent: String, writer: BufferedWriter,
      offset: LocationOffset) = {
    val citiesWithIndex = cities.drop(offset.city).zipWithIndex match {
      case Seq((cityElement, index)) => Seq(CityWithIndex(cityElement, index))
    }
    
    for(cityWithIndex <- citiesWithIndex) {
      val cityUrl = cityWithIndex.cityElement.attr("href")
      val city = cityWithIndex.cityElement.text
      
      writer.write(city + "," + state + "," + country + "," + continent + "," + cityUrl + "\n")
      writer.flush
      
      saveLast(LocationOffset(offset.continent, offset.country, offset.state, offset.city + cityWithIndex.index + 1))
    }
  }
  
  def saveLast(offset: LocationOffset) = {
    val lastWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
    lastWriter.write(offset.continent + "," + offset.country + "," + offset.state + "," + offset.city)
    lastWriter.close
  }
}