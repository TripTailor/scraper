package co.triptailor.hostels.configuration

/**
 * @author lgaleana
 */
object AppConfig {
  object Headers {
    lazy val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5)"
    lazy val accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    lazy val acceptCharset = "ISO-8859-1,utf-8;q=0.7,*;q=0.3"
    lazy val acceptEncoding = "gzip,deflate,sdch"
    lazy val acceptLanguage = "en-US,en;q=0.8"
  }
  
  object JSoup {
    lazy val citiesUrl = "http://www.hostelworld.com/hostels"
  }
  
  object Data {
    lazy val citiesFile = "cities.txt"
    lazy val lastCity = "last_city.txt"
    lazy val data = "data/"
    lazy val lastHostel = "data/last.txt"
  }
}