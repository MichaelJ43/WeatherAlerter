# Weather Alerter App

A Scala application that periodically checks for severe weather alerts from the National Weather Service (NWS) API for a specified location. If severe conditions are met, it can trigger a POST request to a configurable target API endpoint.

## Features

*   Fetches active weather alerts from the NWS API.
*   Determines location:
    *   Accepts latitude and longitude as command-line arguments.
    *   Attempts to determine location via IP address if no arguments are provided.
    *   Falls back to a default location (New York City) if other methods fail.
*   Filters alerts to identify severe warnings based on event type (e.g., "Tornado Warning", "Hurricane Warning") and severity level (e.g., "Severe", "Extreme").
*   If a severe warning is detected, it constructs a JSON payload and can send a POST request to a configurable target API endpoint.
*   Logs activity to the console.
*   (Note: The current `main` method runs once. The previous version had a loop for continuous checking; this can be re-added if desired.)

## Prerequisites

*   Java Development Kit (JDK) 8 or higher
*   Scala (version 2.13.12, as specified in `build.sbt`)
*   sbt (Scala Build Tool)

## Setup and Installation

1.  **Clone the repository (if you haven't already):**
    ```bash
    git clone <your-repository-url>
    cd weatherAlerterApp
    ```

2.  **Compile the project and fetch dependencies:**
    Open your terminal in the project's root directory and run:
    ```bash
    sbt compile
    ```
    This will download all necessary libraries defined in `build.sbt`.

## Configuration

The application requires configuration for the target API endpoint where severe alert notifications will be sent.

Open `src/main/scala/com/example/WeatherAlerter.scala` and modify the following constants within the `WeatherAlerter` object:

*   `TARGET_API_POST_URL`: Set this to the URL of your API endpoint that will receive the POST requests.
    ```scala
    val TARGET_API_POST_URL: String = "YOUR_API_ENDPOINT_URL_HERE" // Replace with your actual URL
    ```
*   `TARGET_API_HEADERS`: Update this map with any required headers for your target API (e.g., `Content-Type`, `Authorization`).
    ```scala
    val TARGET_API_HEADERS: Map[String, String] = Map(
      "Content-Type" -> "application/json",
      "Authorization" -> "Bearer YOUR_API_KEY_HERE" // If your API needs authentication
      // Add other necessary headers
    )
    ```
*   **User-Agent for NWS API**: It's good practice to customize the `User-Agent` string in the `getWeatherAlerts` method:
    ```scala
    // Inside getWeatherAlerts method:
    headers = Map("User-Agent" -> "MyScalaWeatherAlerter/1.0 (contact@example.com)") // Customize this
    ```

## Running the Application

To run the weather alerter, use the sbt command from the project's root directory.

**Option 1: Use IP-based location or default location**
```bash
sbt run
```

**Option 2: Specify latitude and longitude**
```bash
sbt "run <latitude> <longitude>"
# Example for Tuckahoe, VA:
# sbt "run 37.5929 -77.5436"
```

The application will start, determine the location, check for weather alerts once, and then print its findings.

## Running Tests

Unit tests are written using ScalaTest. To execute the tests, run:

```bash
sbt test
```

## Project Structure

```
weatherAlerterApp/
├── .gitignore          # Specifies intentionally untracked files that Git should ignore
├── build.sbt           # sbt build definition file
├── project/            # sbt project specific files (plugins.sbt, build.properties)
├── src/
│   ├── main/
│   │   └── scala/
│   │       └── com/
│   │           └── example/
│   │               └── WeatherAlerter.scala  # Main application logic
│   └── test/
│       └── scala/
│           └── com/
│               └── example/
│                   └── WeatherAlerterSpec.scala # Unit tests (if created)
└── README.md           # This file
```

## Key Dependencies

*   `com.lihaoyi::requests`: For making HTTP requests to the NWS API and the target POST endpoint.
*   `com.lihaoyi::upickle`: For JSON parsing and serialization.
*   `org.scalatest::scalatest`: For writing unit tests.
*   `org.scalatestplus::mockito-4-11`: For using Mockito with ScalaTest.

## License

Please specify your project's license here (e.g., MIT, Apache 2.0). If not specified, it's assumed to be proprietary.
```