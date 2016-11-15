name := "TempehServer"

version := "1.0"

scalaVersion := "2.12.0"

// https://mvnrepository.com/artifact/com.google.code.gson/gson
libraryDependencies += "com.google.code.gson" % "gson" % "2.7"

// https://mvnrepository.com/artifact/org.jsoup/jsoup
libraryDependencies += "org.jsoup" % "jsoup" % "1.10.1"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

mainClass in (Compile, run) := Some("server.tempeh.crawler.Main")