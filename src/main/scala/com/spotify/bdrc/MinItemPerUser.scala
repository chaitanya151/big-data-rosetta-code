/*
 * Copyright 2016 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.bdrc

import com.spotify.bdrc.util.Records.UserItemData
import com.spotify.scio.values.SCollection
import com.twitter.scalding.TypedPipe
import org.apache.spark.rdd.RDD

/**
 * Compute one item with max score per user.
 *
 * Input is a collection of (user, item, score).
 */
object MinItemPerUser {

  def scalding(input: TypedPipe[UserItemData]): TypedPipe[UserItemData] = {
    input
      .groupBy(_.user)
      // pick the side with lower score for each pair
      .reduce((x, y) => if (x.score < y.score) x else y)
      .values
  }

  def scaldingWithAlgebird(input: TypedPipe[UserItemData]): TypedPipe[UserItemData] = {
    import com.twitter.algebird.Aggregator.minBy
    input
      .groupBy(_.user)
      .aggregate(minBy(_.score))
      .values
  }

  def scio(input: SCollection[UserItemData]): SCollection[UserItemData] = {
    input
      .keyBy(_.user)
      .topByKey(1)(Ordering.by(-_.score))
      .flatMap(_._2)
  }

  def scioWithAlgebird(input: SCollection[UserItemData]): SCollection[UserItemData] = {
    import com.twitter.algebird.Aggregator.minBy
    input
      .keyBy(_.user)
      // explicit type due to type inference limitation
      .aggregateByKey(minBy { x: UserItemData => x.score})
      .values
  }

  def spark(input: RDD[UserItemData]): RDD[UserItemData] = {
    input
      .keyBy(_.user)
      .reduceByKey((x: UserItemData, y: UserItemData) => if (x.score < y.score) x else y)
      .values
  }

  def sparkWithAlgebird(input: RDD[UserItemData]): RDD[UserItemData] = {
    import com.twitter.algebird.Aggregator.minBy
    import com.twitter.algebird.spark._
    input
      .keyBy(_.user)
      .algebird
      // explicit type due to type inference limitation
      .aggregateByKey(minBy { x: UserItemData => x.score })
      .values
  }

  def sparkWithMllib(input: RDD[UserItemData]): RDD[UserItemData] = {
    import org.apache.spark.mllib.rdd.MLPairRDDFunctions._
    input
      .keyBy(_.user)
      .topByKey(1)(Ordering.by(-_.score))  // from spark-mllib, inverse ordering
      .flatMap(_._2)
  }

}
