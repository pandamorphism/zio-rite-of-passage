package com.rockthejvm.reviewboard.http.endpoints
import sttp.tapir.*

trait HealthEndpoint {
  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("the health check")
    .get
    .in("health")
    .out(plainBody[String])
}
