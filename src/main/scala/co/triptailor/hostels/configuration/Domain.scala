package co.triptailor.hostels.configuration

import org.jsoup.nodes.Element

/**
 * @author lgaleana
 */
case class LocationOffset(continent: Int, country: Int, state: Int, city: Int)
case class ContinentWithIndex(continent: String, countryElements: Seq[Element], index: Int)
case class CountryWithIndex(countryElement: Element, index: Int)
case class StateWithIndex(stateElement: Element, index: Int)
case class CityWithIndex(cityElement: Element, index: Int)

case class HostelOffset(city: Int, hostel: Int, review: Int)
case class LocationInfo(country: String, city: String, url: String)
case class Review(text: String, meta: String)
case class CityLineWithIndex(cityLine: String, index: Int)
case class HostelUrlWithIndex(url: String, index: Int)
case class ReviewWithIndex(review: Review, index: Int)