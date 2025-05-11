scalaVersion := "2.13.12" // Or any recent Scala 2.13.x or 3.x version

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "requests" % "0.8.0", // For making HTTP requests
  "com.lihaoyi" %% "upickle" % "3.1.3"   // For JSON parsing
)

libraryDependencies ++= Seq(
  // Your existing dependencies like requests and upickle
  "com.lihaoyi" %% "requests" % "0.8.0", // Ensure this matches your project's version
  "com.lihaoyi" %% "upickle" % "3.1.3",  // Ensure this matches your project's version

  // ScalaTest and Mockito for testing
  "org.scalatest" %% "scalatest" % "3.2.17" % "test",
  "org.scalatestplus" %% "mockito-4-11" % "3.2.17.0" % "test" // Uses Mockito 4.11.x
)
