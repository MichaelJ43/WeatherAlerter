package com.example

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockitoEq}
import requests.{RequestFailedException, Response} // For mocking Response properties
import upickle.default._
import upickle.core.AbortException

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Helper trait to capture console output for testing println statements.
 */
trait CapturingConsoleOutput {
  def captureOutput(block: => Unit): String = {
    val outputStream = new ByteArrayOutputStream()
    val utf8 = StandardCharsets.UTF_8.name()
    Console.withOut(outputStream) {
      Console.withErr(outputStream) { // Capture stderr too if needed
        block
      }
    }
    outputStream.toString(utf8).trim
  }
}

class WeatherAlerterSpec extends AnyFunSuite with MockitoSugar with CapturingConsoleOutput {

  // --- Tests for isSevereWarning ---
  test("isSevereWarning should return true for a defined severe event and severity") {
    val props = NWSAlertProperties("Tornado Warning", "Severe", None, None, None, "Immediate", "Observed")
    assert(WeatherAlerter.isSevereWarning(props))
  }

  test("isSevereWarning should return true for another defined severe event and extreme severity") {
    val props = NWSAlertProperties("Hurricane Warning", "Extreme", None, None, None, "Immediate", "Likely")
    assert(WeatherAlerter.isSevereWarning(props))
  }

  test("isSevereWarning should return false if event type is not in SEVERE_EVENT_TYPES") {
    val props = NWSAlertProperties("Special Weather Statement", "Severe", None, None, None, "Expected", "Possible")
    assert(!WeatherAlerter.isSevereWarning(props))
  }

  test("isSevereWarning should return false if severity level is not in MINIMUM_SEVERITY_LEVELS") {
    val props = NWSAlertProperties("Tornado Warning", "Moderate", None, None, None, "Immediate", "Observed")
    assert(!WeatherAlerter.isSevereWarning(props))
  }

  test("isSevereWarning should return false for an event not matching any criteria") {
    val props = NWSAlertProperties("Rain Shower", "Minor", None, None, None, "Expected", "Possible")
    assert(!WeatherAlerter.isSevereWarning(props))
  }

  // --- Tests for getWeatherAlerts ---

  // Note on testing getWeatherAlerts:
  // Directly unit testing methods that use global objects like `requests.get` for external calls
  // is challenging with standard Mockito if you want to mock the `requests.get` call itself
  // without refactoring WeatherAlerter (e.g., to inject a HTTP client service).
  // The tests below will focus on parts of the logic or demonstrate conceptually how one might test
  // if mocking `requests.get` were straightforward.
  // For robust testing of HTTP interactions, consider:
  // 1. Refactoring WeatherAlerter to accept an injectable HTTP client.
  // 2. Using integration tests with a tool like WireMock to simulate the NWS API.
  // 3. Using a Scala mocking library with more advanced capabilities for mocking objects (e.g., ScalaMock).

  val LAT: Double = WeatherAlerter.TUCKAHOE_LAT
  val LON: Double = WeatherAlerter.TUCKAHOE_LON
  val EXPECTED_NWS_API_URL: String = s"https://api.weather.gov/alerts/active?point=$LAT,$LON"

  test("getWeatherAlerts - JSON parsing logic for a successful response") {
    // This test focuses on the JSON parsing part, assuming a valid JSON string is received.
    val rawJsonResponse = """{
      "features": [
        {
          "properties": {
            "event": "Severe Thunderstorm Warning",
            "severity": "Severe",
            "headline": "Severe Thunderstorm Warning for area",
            "description": "A severe thunderstorm is happening.",
            "instruction": "Take cover.",
            "urgency": "Immediate",
            "certainty": "Observed"
          }
        },
        {
          "properties": {
            "event": "Flash Flood Watch",
            "severity": "Moderate",
            "headline": "Flash Flood Watch in effect",
            "description": "Potential for flash flooding.",
            "instruction": "Monitor updates.",
            "urgency": "Expected",
            "certainty": "Likely"
          }
        }
      ]
    }"""
    val parsedResponse = readNWSAlertResponse
    assert(parsedResponse.features.length == 2)
    assert(parsedResponse.features.head.properties.event == "Severe Thunderstorm Warning")
    assert(parsedResponse.features.head.properties.severity == "Severe")
    assert(parsedResponse.features(1).properties.event == "Flash Flood Watch")
  }

  test("getWeatherAlerts - JSON parsing logic for response with no features") {
    val rawJsonResponse = """{"features": []}"""
    val parsedResponse = readNWSAlertResponse
    assert(parsedResponse.features.isEmpty)
  }

  test("getWeatherAlerts - JSON parsing should fail for invalid JSON") {
    val invalidJson = "this is not valid json"
    assertThrows[upickle.core.AbortException] {
      readNWSAlertResponse
    }
  }

  // Conceptual test: How you would test if `requests.get` could be mocked
  // test("getWeatherAlerts should return alerts on successful API call (conceptual)") {
  //   val mockRequestsLib = mock[requests.type] // This kind of mocking is advanced
  //   val mockHttpResponse = mock[requests.Response]
  //   when(mockHttpResponse.statusCode).thenReturn(200)
  //   when(mockHttpResponse.text()).thenReturn("""{"features": [{"properties": {"event": "Test Event", "severity": "Severe", "urgency": "Immediate", "certainty": "Observed"}}]}""")
  //
  //   // This is the tricky part: making WeatherAlerter use mockRequestsLib.get
  //   // when(mockRequestsLib.get(mockitoEq(EXPECTED_NWS_API_URL), headers = any[Map[String, String]])).thenReturn(mockHttpResponse)
  //
  //   // If the above `when` worked and WeatherAlerter used this mocked `requests.get`:
  //   // val alerts = WeatherAlerter.getWeatherAlerts(LAT, LON)
  //   // assert(alerts.nonEmpty)
  //   // assert(alerts.head.properties.event == "Test Event")
  // }

  // --- Tests for triggerPostRequest ---

  // Note on testing triggerPostRequest:
  // Similar to getWeatherAlerts, `TARGET_API_POST_URL` is a `val` in an object, making it hard to change
  // for different test scenarios without reflection or recompilation.
  // Mocking `requests.post` faces the same challenges as `requests.get`.

  val sampleAlertFeature: NWSAlertFeature = NWSAlertFeature(
    NWSAlertProperties(
      event = "Tornado Warning",
      severity = "Extreme",
      headline = Some("Tornado Warning Issued"),
      description = Some("A tornado is on the ground."),
      instruction = Some("Seek shelter immediately!"),
      urgency = "Immediate",
      certainty = "Observed"
    )
  )

  test("triggerPostRequest should print warning and not send POST if TARGET_API_POST_URL is default") {
    // This test relies on the default value of TARGET_API_POST_URL.
    // If it's changed from "YOUR_API_ENDPOINT_URL_HERE", this test might behave differently.
    val originalPostUrl = WeatherAlerter.TARGET_API_POST_URL
    val originalHeaders = WeatherAlerter.TARGET_API_HEADERS

    // Temporarily (for this conceptual block) assume we could set it, or check current state
    if (originalPostUrl == "YOUR_API_ENDPOINT_URL_HERE") {
      val output = captureOutput {
        WeatherAlerter.triggerPostRequest(sampleAlertFeature)
      }
      assert(output.contains("SEVERE WARNING DETECTED: Tornado Warning - Tornado Warning Issued"))
      assert(output.contains("!!! POST Request not sent: TARGET_API_POST_URL is not configured. !!!"))
      assert(output.contains("Payload that would be sent:"))

      // To verify `requests.post` was NOT called, you'd ideally use a mock of the `requests` object.
    } else {
      info(s"Skipping 'triggerPostRequest default URL' test because TARGET_API_POST_URL is configured to: $originalPostUrl")
    }
  }

  test("triggerPostRequest payload construction") {
    // Test the payload string generation part, which is independent of the actual POST call.
    // This is a bit of a white-box test for the current payload structure.
    val alert = NWSAlertFeature(NWSAlertProperties("Test Event", "Test Severity", None, None, None, "Test Urgency", "Test Certainty"))
    
    // Capture the output to check the payload string if TARGET_API_POST_URL is default
    if (WeatherAlerter.TARGET_API_POST_URL == "YOUR_API_ENDPOINT_URL_HERE") {
        val output = captureOutput {
            WeatherAlerter.triggerPostRequest(alert)
        }
        // Example: Check if timestamp is in the payload string
        // This is brittle if the payload format changes often.
        assert(output.contains("\"timestamp\":\"")) 
    } else {
        // If URL is configured, we can't easily see the payload without mocking the post.
        // However, we can reconstruct what the payload *should* be.
        val payload = ujson.Obj("timestamp" -> "dummy") // only timestamp is in current payload
        val payloadString = write(payload)
        // This doesn't directly test what's inside triggerPostRequest's payload construction
        // without being able to intercept it.
        // A better test would be if payload construction was a separate function.
        info("Payload construction test is limited when TARGET_API_POST_URL is configured, as POST is attempted.")
    }
  }

  // Conceptual test: How you would test if `requests.post` could be mocked and URL configured
  // test("triggerPostRequest should attempt POST if URL is configured (conceptual)") {
  //   // Assume TARGET_API_POST_URL is set to "http://my-test-api.com" for this test scenario
  //   // And TARGET_API_HEADERS are set appropriately.
  //   // This would require a way to change these vals for the test, or use a different WeatherAlerter instance.
  //
  //   val mockRequestsLib = mock[requests.type] // Advanced mocking
  //   val mockHttpResponse = mock[requests.Response]
  //   when(mockHttpResponse.statusCode).thenReturn(200)
  //   when(mockHttpResponse.text()).thenReturn("""{"status": "received"}""")
  //
  //   // when(mockRequestsLib.post(mockitoEq("http://my-test-api.com"), headers = any[Map[String,String]](), data = anyString()))
  //   //  .thenReturn(mockHttpResponse)
  //
  //   // If WeatherAlerter used this mock:
  //   // val output = captureOutput { WeatherAlerter.triggerPostRequest(sampleAlertFeature) }
  //   // assert(output.contains("POST request successful: 200"))
  //   // verify(mockRequestsLib).post(mockitoEq("http://my-test-api.com"), any[Map[String,String]](), anyString())
  // }
}
