package co.triptailor.hostels.cities

import co.triptailor.hostels.Scraper
import co.triptailor.hostels.AppConfig
import java.io.FileWriter
import java.io.BufferedWriter
import org.jsoup.nodes.Element
import scala.collection.JavaConverters._
import java.io.BufferedReader
import java.io.FileReader

/**
 * @author lgaleana
 */
object CitiesScraper extends Scraper {
  def main(args: Array[String]): Unit = {
    scrape(AppConfig.JSoup.citiesUrl)
  }
  
  def scrape(url: String): Unit = {
    val reader = new BufferedReader(new FileReader(AppConfig.Data.lastCity))
    val offsetContainer = reader.readLine().split(",").map(_.toInt)
    reader.close
    val offset = (offsetContainer(0), offsetContainer(1), offsetContainer(2), offsetContainer(3))
    
    try {
      scrapeCities(AppConfig.JSoup.citiesUrl, offset)
    } catch {
      case ex: java.net.SocketTimeoutException => {
        System.err.println(ex.getStackTrace)
        Thread.sleep(10000)
        scrape(url)
      }
    }
  }
  
  def scrapeCities(url: String, offset: (Int, Int, Int, Int)) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    val countriesContainer = doc.select(".toprated.rounded").get(0)
    val continentCountriesPairs = countriesContainer.children().asScala.sliding(2, 2).map(x => (x(0), x(1))).toSeq
    
    val writer = new BufferedWriter(new FileWriter(AppConfig.Data.citiesFile, true))
    for {
      i <- 0 to continentCountriesPairs.size - 1
      if(i >= offset._1)
      continentCountries = continentCountriesPairs.drop(i).head
      continent = continentCountries._1.child(1).text
      countryElements = continentCountries._2.select("a").asScala
      countryOffset = if(offset._1 == i) offset._2 else 0
      stateOffset = if(offset._1 == i) offset._3 else 0
      cityOffset = if(offset._1 == i) offset._4 else 0
    } {
      scrapeContinent(continent, countryElements, writer, (i, countryOffset, stateOffset, cityOffset))
      
      val offsetWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
      offsetWriter.write((i + 1) + "0,0,0")
      offsetWriter.close
    }
    writer.close
  }
  
  def scrapeContinent(continent: String, countryElements: Seq[Element], writer: BufferedWriter,
      offset: (Int, Int, Int, Int)) = {
    for {
      i <- 0 to countryElements.size - 1
      if(i >= offset._2)
      countryElement = countryElements.drop(i).head
      countryUrl = countryElement.attr("href")
      country = countryElement.text
      stateOffset = if(offset._2 == i) offset._2 else 0
      cityOffset = if(offset._2 == i) offset._3 else 0
    } {
      scrapeCountry(countryUrl, country, continent, writer, (offset._1, i, stateOffset, cityOffset))
      
      val offsetWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
      offsetWriter.write(offset._1 + "," + (i + 1) + ",0,0")
      offsetWriter.close
    }
  }
  
  def scrapeCountry(url: String, country: String, continent: String, writer: BufferedWriter,
      offset: (Int, Int, Int, Int)) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    val cities = doc.getElementById("bottomlist").select("a").asScala
    if(!cities.isEmpty) {
      saveCities(cities, "", country, continent, writer, offset)
      
      val offsetWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
      offsetWriter.write(offset._1 + "," + offset._2 + ",0,0")
      offsetWriter.close
    }
    else {
      val states = doc.getElementById("states").select("a").asScala
      for {
        i <- 0 to states.size - 1
        if(i >= offset._3)
        stateElement = states.drop(i).head
        state = stateElement.text
        cities = scrapeStateForCities(stateElement.attr("href"))
        cityOffset = if(offset._3 == i) offset._3 else 0
      } {
        saveCities(cities, state, country, continent, writer, (offset._1, offset._2, i, cityOffset))
        
        val offsetWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
        offsetWriter.write(offset._1 + "," + offset._2 + "," +  (i + 1) + ",0")
        offsetWriter.close
      }
    }
  }
  
  def scrapeStateForCities(url: String) = {
    println("Scraping " + url)
    val doc = getDocument(url)
    doc.getElementById("bottomlist").select("a").asScala
  }
  
  def saveCities(cities: Seq[Element], state: String, country: String, continent: String, writer: BufferedWriter,
      offset: (Int, Int, Int, Int)) = {
    for {
      i <- 0 to cities.size - 1
      if(offset._4 >= i)
      cityElement <- cities
      cityUrl = cityElement.attr("href")
      city = cityElement.text
    } {
      writer.write(city + "," + state + "," + country + "," + continent + "," + cityUrl + "\n")
      writer.flush
      
      val offsetWriter = new BufferedWriter(new FileWriter(AppConfig.Data.lastCity))
      offsetWriter.write(offset._1 + "," +  offset._2 + "," + offset._3 + "," + (i + 1))
      offsetWriter.close
    }
  }
}