package com.example

import requests._
import upickle.default._

// Define case classes to parse the JSON response from NWS API
// See NWS API documentation for full structure: https://www.weather.gov/documentation/services-web-api
case class NWSPoint(coordinates: Seq[Double])
case class NWSGeometry(geometry: NWSPoint) // Simplified, assuming point geometry for alerts

case class NWSAlertProperties(
  event: String,          // e.g., "Tornado Warning", "Severe Thunderstorm Warning"
  severity: String,       // e.g., "Severe", "Extreme", "Moderate"
  headline: Option[String],
  description: Option[String],
  instruction: Option[String],
  urgency: String,        // e.g., "Immediate", "Expected"
  certainty: String       // e.g., "Observed", "Likely"
)
// Add implicit readers for upickle
object NWSAlertProperties {
  implicit val rw: ReadWriter[NWSAlertProperties] = macroRW
}

case class NWSAlertFeature(
  properties: NWSAlertProperties
)
// Add implicit readers for upickle
object NWSAlertFeature {
  implicit val rw: ReadWriter[NWSAlertFeature] = macroRW
}

case class NWSAlertResponse(
  features: Seq[NWSAlertFeature]
)
// Add implicit readers for upickle
object NWSAlertResponse {
  implicit val rw: ReadWriter[NWSAlertResponse] = macroRW
}


object WeatherAlerter {


  // NWS API endpoint for active alerts by point
  val NWS_API_URL_TEMPLATE: String = "https://api.weather.gov/alerts/active?point={lat},{lon}"

  // Define what constitutes a "severe" weather warning for your use case
  val SEVERE_EVENT_TYPES: Set[String] = Set(
    "Tornado Warning",
    "Severe Thunderstorm Warning",
    "Flash Flood Warning",
    "Hurricane Warning",
    "Tsunami Warning",
    "Extreme Wind Warning",
    "Blizzard Warning",
    "Ice Storm Warning"
    // Add or remove event types as needed
  )
  val MINIMUM_SEVERITY_LEVELS: Set[String] = Set("Severe", "Extreme") // NWS uses "Severe", "Extreme", "Moderate", "Minor"

  // --- !!! IMPORTANT: Customize this section for your POST request !!! ---
  val TARGET_API_POST_URL: String = "YOUR_API_ENDPOINT_URL_HERE" // e.g., "https://api.example.com/notify"
  val TARGET_API_HEADERS: Map[String, String] = Map(
    "Content-Type" -> "application/json",
    "Authorization" -> "Bearer YOUR_API_KEY_HERE" // If your API needs authentication
    // Add other necessary headers
  )
  // --- End of customization section ---


  /**
   * Fetches active weather alerts from the NWS API for the given coordinates.
   *
   * @param lat Latitude
   * @param lon Longitude
   * @return A sequence of NWSAlertFeature, or an empty sequence if an error occurs or no alerts.
   */
  def getWeatherAlerts(lat: Double, lon: Double): Seq[NWSAlertFeature] = {
    val apiUrl = NWS_API_URL_TEMPLATE.replace("{lat}", lat.toString).replace("{lon}", lon.toString)
    println(s"Fetching weather alerts from: $apiUrl")

    try {
      // NWS API requires a User-Agent header.
      // Replace "MyWeatherApp/1.0 (myemail@example.com)" with your app's name and contact.
      val response = requests.get(
        apiUrl,
        headers = Map("User-Agent" -> "MyScalaWeatherAlerter/1.0 (contact@example.com)")
      )

      if (response.statusCode == 200) {
        val rawJson = response.text()
        // println(s"Raw JSON response: $rawJson") // For debugging
        val parsedResponse = read[NWSAlertResponse](rawJson)
        parsedResponse.features
      } else {
        println(s"Error fetching weather alerts: ${response.statusCode} - ${response.statusMessage}")
        println(s"Response body: ${response.text()}")
        Seq.empty[NWSAlertFeature]
      }
    } catch {
      case e: requests.RequestFailedException =>
        println(s"Request failed: ${e.response.statusCode} ${e.response.statusMessage}")
        println(s"Response body: ${e.response.text()}")
        Seq.empty[NWSAlertFeature]
      case e: upickle.core.AbortException =>
        println(s"JSON parsing error: ${e.getMessage}")
        // You might want to log the raw JSON here if debugging parsing
        Seq.empty[NWSAlertFeature]
      case e: Exception =>
        println(s"An unexpected error occurred while fetching alerts: ${e.getMessage}")
        e.printStackTrace()
        Seq.empty[NWSAlertFeature]
    }
  }

  /**
   * Checks if a given alert is considered a severe warning.
   *
   * @param alert The NWSAlertFeature to check.
   * @return true if the alert is severe, false otherwise.
   */
  def isSevereWarning(alertProperties: NWSAlertProperties): Boolean = {
    val eventTypeMatch = SEVERE_EVENT_TYPES.contains(alertProperties.event)
    val severityMatch = MINIMUM_SEVERITY_LEVELS.contains(alertProperties.severity)
    // You might also want to check urgency (e.g., "Immediate") and certainty (e.g., "Observed", "Likely")
    // val urgencyMatch = alertProperties.urgency == "Immediate"
    // val certaintyMatch = Set("Observed", "Likely").contains(alertProperties.certainty)

    eventTypeMatch && severityMatch // && urgencyMatch && certaintyMatch (if you add these checks)
  }

  /**
   * Triggers a POST request to your target API.
   * !!! YOU MUST CUSTOMIZE THE URL, HEADERS, AND PAYLOAD FORMATTING !!!
   *
   * @param alert The severe alert that triggered this action.
   */
  def triggerPostRequest(alert: NWSAlertFeature): Unit = {
    println(s"SEVERE WARNING DETECTED: ${alert.properties.event} - ${alert.properties.headline.getOrElse("No headline")}")
    println("Attempting to send POST request...")

    // --- !!! Customize the payload based on your target API's requirements !!! ---
    // This is just an example payload.
    val payload = ujson.Obj(
      // "warningType" -> alert.properties.event,
      // "severity" -> alert.properties.severity,
      // "headline" -> alert.properties.headline.getOrElse("N/A"),
      // "description" -> alert.properties.description.getOrElse("N/A"),
      // "instruction" -> alert.properties.instruction.getOrElse("N/A"),
      "timestamp" -> java.time.Instant.now().toString
    )
    val payloadString = write(payload)
    // --- End of payload customization ---

    if (TARGET_API_POST_URL == "YOUR_API_ENDPOINT_URL_HERE") {
      println("!!! POST Request not sent: TARGET_API_POST_URL is not configured. !!!")
      println(s"Payload that would be sent: $payloadString")
      return
    }

    try {
      println(s"Sending POST request to $TARGET_API_POST_URL with payload: $payloadString")
      
    //   val response = requests.post(
    //     TARGET_API_POST_URL,
    //     headers = TARGET_API_HEADERS,
    //     data = payloadString
    //   )

    //   if (response.statusCode >= 200 && response.statusCode < 300) {
    //     println(s"POST request successful: ${response.statusCode}")
    //     println(s"Response: ${response.text()}")
    //   } else {
    //     println(s"POST request failed: ${response.statusCode} - ${response.statusMessage}")
    //     println(s"Response body: ${response.text()}")
    //   }
    } catch {
      case e: requests.RequestFailedException =>
        println(s"POST request failed: ${e.response.statusCode} ${e.response.statusMessage}")
        println(s"Response body: ${e.response.text()}")
      case e: Exception =>
        println(s"An error occurred while sending POST request: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  def main(args: Array[String]): Unit = {
    println("Starting Weather Alerter...")

    var latOpt: Option[Double] = None
    var lonOpt: Option[Double] = None

    // Try to parse latitude and longitude from command line arguments
    if (args.length == 2) {
      try {
        latOpt = Some(args(0).toDouble)
        lonOpt = Some(args(1).toDouble)
        println(s"Using location from command line arguments: Latitude=${latOpt.get}, Longitude=${lonOpt.get}")
      } catch {
        case e: NumberFormatException =>
          println("Error: Invalid command-line arguments for latitude/longitude. Must be numbers.")
          println("Attempting to use IP-based location or fallback to default.")
      }
    } else if (args.length > 0 && args.length != 2) {
      println("Usage: WeatherAlerter [latitude longitude]")
      println("If no arguments are provided, IP-based location or a default will be used.")
    }

    // If location not provided via args, try IP-based lookup
    if (latOpt.isEmpty || lonOpt.isEmpty) {
      println("Attempting to determine location via IP address...")
      val ipApiUrl = "http://ip-api.com/json"
      try {
        val locationResponse = requests.get(ipApiUrl, connectTimeout = 5000, readTimeout = 5000) // Added timeouts
        if (locationResponse.statusCode == 200) {
          val locationJson = ujson.read(locationResponse.text())
          latOpt = Some(locationJson("lat").num)
          lonOpt = Some(locationJson("lon").num)
          val city = locationJson.obj.get("city").map(_.str).getOrElse("N/A")
          val regionName = locationJson.obj.get("regionName").map(_.str).getOrElse("N/A")
          println(s"Location determined by IP: $city, $regionName (Latitude=${latOpt.get}, Longitude=${lonOpt.get})")
        } else {
          println(s"Error getting location from IP API: ${locationResponse.statusCode} - ${locationResponse.statusMessage}")
          // Fallback to default if IP lookup also fails
        }
      } catch {
        case e: Exception =>
          println(s"Failed to get location from IP API: ${e.getMessage}")
          // Fallback to default if IP lookup also fails
      }
    }

    // If still no location, use default (or exit if preferred)
    if (latOpt.isEmpty || lonOpt.isEmpty) {
      println("Using default location (New York City, NY).")
      latOpt = Some(40.7128) // New York City latitude
      lonOpt = Some(-74.0060) // New York City longitude
    } 

    val finalLat = latOpt.get
    val finalLon = lonOpt.get

    println(s"\n--- Checking for alerts at ${java.time.LocalTime.now()} for Latitude=$finalLat, Longitude=$finalLon ---")
    val activeAlerts = getWeatherAlerts(finalLat, finalLon)

    if (activeAlerts.isEmpty) {
      println("No active weather alerts found for the specified location.")
    } else {
      println(s"Found ${activeAlerts.length} active alert(s). Checking for severe warnings...")

      val severeWarnings = activeAlerts.filter(alert => isSevereWarning(alert.properties))

      if (severeWarnings.isEmpty) {
        println("No severe warnings meeting the criteria.")
      } else {
        println(s"Found ${severeWarnings.length} severe warning(s):")
        severeWarnings.foreach { alert =>
          triggerPostRequest(alert)
        }
      }
    }
    println("Weather Alerter finished.")
  }
}
