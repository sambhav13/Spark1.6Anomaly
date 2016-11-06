package anomaly

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

class MainFile {
  
}

object MainFile{
  
  def main(args:Array[String]) ={
    
    
    val conf= new SparkConf().setMaster("local").setAppName("AnomalyApp")
    val sc = new SparkContext(conf)
    
    
   val data_t = sc.textFile("data.csv")  
    val crossData = sc.textFile("cross.txt")
    
    val header = data_t.first()
    //extract header
    val data = data_t.filter(row => row != header)
    data.collect().foreach(x => println(x))
    
    
    
   /* val data = sc.textFile("training.csv")  
    val crossData = sc.textFile("cross_val.csv")*/
    
    
    //val tempData  = data.map { x => x.split(",") }.map { x => x.tt }
    
    val tempData = data.map(_.split(",").map(_.toDouble))
    val trainData = tempData.map(x=>Vectors.dense(x))
    
    val tempcrossData = crossData.map(_.split(",").map(_.toDouble))
    val tempCross = tempcrossData.map( x => new LabeledPoint(x(0),Vectors.dense( x.slice(1, x.length) ) ) )
    
    val ad = new AnomalyDetect()
    val model =  ad.run(trainData)
    
    
    // val cvVec = trainData.map(_.features)
   //val results  =  model.predict(trainData)
   
   val features = tempCross.map(_.features)
   
   val optimizeModel = ad.optimize(tempCross,model)
   
   val results =  optimizeModel.predict(features)
   val outliers = results.filter(_._2).collect()
   outliers.foreach(o => println(o._1))
   println("\nFound %s outliers\n".format(outliers.length))
    sc.stop()
    
    
    
  }
}