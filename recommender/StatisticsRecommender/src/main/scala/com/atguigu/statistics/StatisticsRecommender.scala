package com.atguigu.statistics

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * Copyright (c) 2018-2028 尚硅谷 All Rights Reserved 
  *
  * Project: MovieRecommendSystem
  * Package: com.atguigu.statistics
  * Version: 1.0
  *
  * Created by wushengran on 2019/4/2 10:00
  */

case class Movie(mid: Int, name: String, descri: String, timelong: String, issue: String,
                 shoot: String, language: String, genres: String, actors: String, directors: String)

case class Rating(uid: Int, mid: Int, score: Double, timestamp: Int )

case class MongoConfig(uri:String, db:String)

// 定义一个基准推荐对象
case class Recommendation( mid: Int, score: Double )

// 定义电影类别top10推荐对象
case class GenresRecommendation( genres: String, recs: Seq[Recommendation] )

object StatisticsRecommender {

  // 定义表名
  val MONGODB_MOVIE_COLLECTION = "Movie"
  val MONGODB_RATING_COLLECTION = "Rating"

  //统计的表的名称
  val RATE_MORE_MOVIES = "RateMoreMovies"
  val RATE_MORE_RECENTLY_MOVIES = "RateMoreRecentlyMovies"
  val AVERAGE_MOVIES = "AverageMovies"
  val GENRES_TOP_MOVIES = "GenresTopMovies"

  def main(args: Array[String]): Unit = {
    val config = Map(
      "spark.cores" -> "local[*]",
      "mongo.uri" -> "mongodb://localhost:27017/recommender",
      "mongo.db" -> "recommender"
    )

    // 创建一个sparkConf
    val sparkConf = new SparkConf().setMaster(config("spark.cores")).setAppName("StatisticsRecommeder")

    // 创建一个SparkSession
    val spark = SparkSession.builder().config(sparkConf).getOrCreate()

    import spark.implicits._

    implicit val mongoConfig = MongoConfig(config("mongo.uri"), config("mongo.db"))

    // 从mongodb加载数据
    val ratingDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_RATING_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Rating]
      .toDF()

    val movieDF = spark.read
      .option("uri", mongoConfig.uri)
      .option("collection", MONGODB_MOVIE_COLLECTION)
      .format("com.mongodb.spark.sql")
      .load()
      .as[Movie]
      .toDF()

    // 创建名为ratings的临时表
    ratingDF.createOrReplaceTempView("ratings")

    // 不同的统计推荐结果
    // 1. 历史热门统计，历史评分数据最多，mid，count
    val rateMoreMoviesDF = spark.sql("select mid, count(mid) as count from ratings group by mid")
    // 把结果写入对应的mongodb表中
    storeDFInMongoDB( rateMoreMoviesDF, RATE_MORE_MOVIES )

    // 2. 近期热门统计，按照“yyyyMM”格式选取最近的评分数据，统计评分个数
    // 创建一个日期格式化工具
    val simpleDateFormat = new SimpleDateFormat("yyyyMM")
    // 注册udf，把时间戳转换成年月格式
    spark.udf.register("changeDate", (x: Int)=>simpleDateFormat.format(new Date(x * 1000L)).toInt )

    // 对原始数据做预处理，去掉uid
    val ratingOfYearMonth = spark.sql("select mid, score, changeDate(timestamp) as yearmonth from ratings")
    ratingOfYearMonth.createOrReplaceTempView("ratingOfMonth")

    // 从ratingOfMonth中查找电影在各个月份的评分，mid，count，yearmonth
    val rateMoreRecentlyMoviesDF = spark.sql("select mid, count(mid) as count, yearmonth from ratingOfMonth group by yearmonth, mid order by yearmonth desc, count desc")

    // 存入mongodb
    storeDFInMongoDB(rateMoreRecentlyMoviesDF, RATE_MORE_RECENTLY_MOVIES)

    // 3. 优质电影统计，统计电影的平均评分，mid，avg
    val averageMoviesDF = spark.sql("select mid, avg(score) as avg from ratings group by mid")
    storeDFInMongoDB(averageMoviesDF, AVERAGE_MOVIES)

    // 4. 各类别电影Top统计
    // 定义所有类别
    val genres = List("Action","Adventure","Animation","Comedy","Crime","Documentary","Drama","Family","Fantasy","Foreign","History","Horror","Music","Mystery"
      ,"Romance","Science","Tv","Thriller","War","Western")

    // 把平均评分加入movie表里，加一列，inner join
    val movieWithScore = movieDF.join(averageMoviesDF, "mid")

    // 为做笛卡尔积，把genres转成rdd
    val genresRDD = spark.sparkContext.makeRDD(genres)

    // 计算类别top10，首先对类别和电影做笛卡尔积
    val genresTopMoviesDF = genresRDD.cartesian(movieWithScore.rdd)
      .filter{
        // 条件过滤，找出movie的字段genres值(Action|Adventure|Sci-Fi)包含当前类别genre(Action)的那些
        case (genre, movieRow) => movieRow.getAs[String]("genres").toLowerCase.contains( genre.toLowerCase )
      }
      .map{
        case (genre, movieRow) => ( genre, ( movieRow.getAs[Int]("mid"), movieRow.getAs[Double]("avg") ) )
      }
      .groupByKey()
      .map{
        case (genre, items) => GenresRecommendation( genre, items.toList.sortWith(_._2>_._2).take(10).map( item=> Recommendation(item._1, item._2)) )
      }
      .toDF()

    storeDFInMongoDB(genresTopMoviesDF, GENRES_TOP_MOVIES)

    spark.stop()
  }

  def storeDFInMongoDB(df: DataFrame, collection_name: String)(implicit mongoConfig: MongoConfig): Unit ={
    df.write
      .option("uri", mongoConfig.uri)
      .option("collection", collection_name)
      .mode("overwrite")
      .format("com.mongodb.spark.sql")
      .save()
  }

}
