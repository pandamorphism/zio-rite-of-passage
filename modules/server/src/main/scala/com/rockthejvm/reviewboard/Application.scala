package com.rockthejvm.reviewboard
import zio.*
import zio.http.Server
import zio.json.{DeriveJsonCodec, JsonCodec}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import com.rockthejvm.reviewboard.http.controllers.HealthController
import com.rockthejvm.reviewboard.http.controllers.CompanyController
import com.rockthejvm.reviewboard.http.HttpAPI
import com.rockthejvm.reviewboard.services.CompanyService

object Application extends ZIOAppDefault:

  val simpleServerProgram =
    for
      endpoints <- HttpAPI.endpointsZIO
      _         <- Server.serve(
                     ZioHttpInterpreter(
                       ZioHttpServerOptions.default // can add configs e.g. CORS
                     ).toHttp(endpoints)
                   )
    yield ()

  override def run = simpleServerProgram
    .flatMap(_ => Console.printLine("Rock the JVM Review Board is running..."))
    .provide(
      Server.default,
      CompanyService.dummyLayer
    )
