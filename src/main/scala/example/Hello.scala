package example

import zio.App
import zio.ZEnv
import zio.ZIO
import zhttp.TLSServer
import zhttp.HttpRouter
import zhttp.MyLogging.MyLogging
import zhttp.MyLogging
import zhttp.LogLevel
import zhttp.HttpRoutes
import zhttp.dsl._
import zhttp.Response
import zhttp.Method._

object ServerExample extends zio.App {


  object param1 extends QueryParam("param1")

  def run(args: List[String]) = {


    val r =  HttpRoutes.of {

       case GET ->  Root / "user" :? param1( par ) =>
        ZIO( Response.Ok  ) 

       case GET -> Root / "health" =>
        ZIO(Response.Ok.asTextBody("Health Check Ok"))
    }

    type MyEnv = MyLogging

    val myHttp = new TLSServer[MyEnv]
    val myHttpRouter = new HttpRouter[MyEnv]
    
    myHttpRouter.addAppRoute( r )

    myHttp.KEYSTORE_PATH = "keystore.jks"
    myHttp.KEYSTORE_PASSWORD = "password"

    myHttp.TLS_PROTO = "TLSv1.2"
    myHttp.BINDING_SERVER_IP = "0.0.0.0"
    myHttp.KEEP_ALIVE = 2000
    myHttp.SERVER_PORT = 8443


    val logger_L = MyLogging.make(("console" -> LogLevel.Trace), ("access" -> LogLevel.Info))

    myHttp.run( myHttpRouter.route ).provideSomeLayer[ZEnv](logger_L).exitCode


  }
}
