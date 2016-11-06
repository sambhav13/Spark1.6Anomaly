package anomaly

import org.apache.spark.mllib.linalg._
import org.apache.spark.rdd.RDD

class AnomalyDetectionModel (val means:Vector,val variances:Vector,val epsilon:Double) extends Serializable{
  
  def predict(point:Vector):Boolean = {
    return AnomalyDetect.predict(point,means,variances,epsilon)
  }
  
  def predict(points:RDD[Vector]):RDD[(Vector,Boolean)] = {
   points.map(p => (p,AnomalyDetect.predict(p,means,variances,epsilon)))
  }
}