package com.rockthejvm.reviewboard.http.controllers
import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import zio.*
import sttp.tapir.server.ServerEndpoint
class HealthController private extends BaseController with HealthEndpoint:
  
  override val routes                   = List(health)
 
  def health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

object HealthController:
  val makeZIO = ZIO.succeed(new HealthController())
