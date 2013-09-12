name := "ConspireChallenge"

version := "1.0"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" % "play_2.10" % "2.2.0-RC1",
  "joda-time" % "joda-time" % "2.2",
  "commons-codec" % "commons-codec" % "1.8"
)
