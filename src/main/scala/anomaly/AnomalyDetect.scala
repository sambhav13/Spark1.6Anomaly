package anomaly

import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.sql.catalyst.expressions.Sqrt
import org.apache.spark.mllib.regression.LabeledPoint

class AnomalyDetect {
  
  val default_epsilon :Double = 0.1
  
  def run(data:RDD[Vector]):AnomalyDetectionModel ={
    
    val stats =  Statistics.colStats(data)
    val means = stats.mean
    val variances = stats.variance
    new AnomalyDetectionModel(means,variances,default_epsilon)
  }
  
  def optimize(crossValData:RDD[LabeledPoint],ad:AnomalyDetectionModel):AnomalyDetectionModel = {
    
    val sc = crossValData.sparkContext
    val bc_mean = sc.broadcast(ad.means)
    val bc_variance = sc.broadcast(ad.variances)
    
    val probsCV: RDD[Double] = crossValData.map { lp => AnomalyDetect.probFunction(lp.features, bc_mean.value,
        bc_variance.value)}
    
    val epsilonWithF1score = evaluate(crossValData,probsCV)
    return new AnomalyDetectionModel(ad.means,ad.variances,epsilonWithF1score._1)
  }
  
  def evaluate(crossValData:RDD[LabeledPoint],probsCV:RDD[Double]) ={
    
    val minp = probsCV.min()
    val maxp = probsCV.max()
    
    val stepSize = (maxp - minp)/10.0
    
    var bestEpsilon = 0D
    var bestF1 = 0D
    
    val sc = crossValData.sparkContext
    
   
    for(e <- minp to maxp by stepSize){
      
       val b_epsilon = sc.broadcast(e)
      val ourPrediction = probsCV.map { p => 
      
                    if(p<b_epsilon.value){
                      1.0
                    }else{
                      0.0
                    }
              
       }
       
       val labelAndPredictions:RDD[(Double,Double)] = crossValData.map(_.label).zip(ourPrediction)
         
         val falsePositives = countStats(labelAndPredictions,0.0,1.0)
         val truePositives = countStats(labelAndPredictions,1.0,1.0)
         val falseNegatives = countStats(labelAndPredictions,1.0,0.0)
        
         val precision = truePositives /Math.max(1.0,falsePositives+truePositives)
         val recall = truePositives /Math.max(1.0,falseNegatives+truePositives)
           
         val f1Score = 2.0 * precision* recall /(precision+recall)
         if(f1Score>bestF1){
           bestF1 = f1Score
           bestEpsilon = e
         }
    }
    
    (bestEpsilon,bestF1)
  }
  
  def countStats(labelPreds:RDD[(Double,Double)],labelValue :Double,predictionValue:Double):Double={
    labelPreds.filter{lp =>
      val label = lp._1
      val pred = lp._2
      (label==labelValue)&&(pred==predictionValue)
    }.count().toDouble
  }
}

object AnomalyDetect{
  def predict(point:Vector,means:Vector,variances:Vector,epsilon:Double):Boolean={
    return probFunction(point,means,variances) < epsilon
  }
  
   def probFunction(point:Vector,means:Vector,variances:Vector):Double={
    
    val triplet :List[(Double,Double,Double)] = (point.toArray,means.toArray,variances.toArray).zipped.toList
    triplet.map{ t => 
            val x =  t._1
            val mean = t._2
            val variance = t._3
            var expValue = Math.pow(Math.E,-0.5 * Math.pow(x - mean,2)/variance)
        (1.0)/(Math.sqrt(variance)*Math.sqrt(2*Math.PI))*expValue
        
    }.product
   /* triplet.map{ trip =>
      val x = trip._1
      val mean = t._2
      val variance = t._3
      var exp = Math.pow(Math.E,-0.5 * Math.pow(x - mean,2)/variance)
        (1.0)/(Math.sqrt(variance)*Math.sqrt(2*Math.PI))*exp
    }*/
    
   // return 0D
  }
}