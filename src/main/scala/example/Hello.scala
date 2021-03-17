package example

import zio.App
import zio.ZEnv
import zio.ZIO
import zio.json._

import zhttp.TLSServer
import zhttp.HttpRouter
import zhttp.MyLogging.MyLogging
import zhttp.MyLogging
import zhttp.LogLevel
import zhttp.HttpRoutes
import zhttp.dsl._
import zhttp.Response
import zhttp.Method._


object UserRecord {
  implicit val decoder: JsonDecoder[UserRecord] = DeriveJsonDecoder.gen[UserRecord]
  implicit val encoder: JsonEncoder[UserRecord] = DeriveJsonEncoder.gen[UserRecord]
}
case class UserRecord(val uid: String )

//Please see URL, for more examples/use cases.
//https://github.com/ollls/zio-tls-http/blob/dev/examples/start/src/main/scala/MyServer.scala

object ServerExample extends zio.App {

  object param1 extends QueryParam( "param1")

  def run(args: List[String]) = {


    val r =  HttpRoutes.of {
       case GET -> Root / "health" =>
        ZIO(Response.Ok().asTextBody("Health Check Ok"))

       case GET -> Root / "user" :? param1( par ) => ZIO( Response.Ok().asTextBody( "param1=" + par ))

       case req @ POST -> Root / "test" =>
         for {
           rec <- ZIO( req.fromJSON[UserRecord] )
           _   <- MyLogging.info("my_application", "UID received: " + rec.uid )
         } yield( Response.Ok().asTextBody( "OK " + rec.uid ) )
    }



    type MyEnv = MyLogging

    val myHttp = new TLSServer[MyEnv]( port = 8443, 
                                      keepAlive = 4000, 
                                      serverIP = "0.0.0.0", 
                                      keystore = "keystore.jks", "password", 
                                      tlsVersion = "TLSv1.2" )

    val myHttpRouter = new HttpRouter[MyEnv]( r )
    

    val logger_L = MyLogging.make( maxLogSize = 1024*1024, maxLogFiles = 7,
                                   ("console" -> LogLevel.Trace), 
                                   ("access" -> LogLevel.Info), 
                                   ( "my_application" -> LogLevel.Info) )

    myHttp.run( myHttpRouter.route ).provideSomeLayer[ZEnv](logger_L).exitCode


  }
}
