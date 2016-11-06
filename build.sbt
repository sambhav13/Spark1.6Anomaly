name := "anomaly-detection"

version := "1.0"

scalaVersion := "2.10.6"

libraryDependencies += "org.apache.spark" %% "spark-core" % "1.6.0"
libraryDependencies += "com.datastax.spark" % "spark-cassandra-connector_2.10" % "1.6.0"
libraryDependencies += "junit" % "junit" % "4.12"
libraryDependencies += "org.apache.spark" % "spark-mllib_2.10" % "1.6.0"



