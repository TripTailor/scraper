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