package com.rockthejvm.reviewboard.http
import com.rockthejvm.reviewboard.http.controllers.*
import zio.*

object HttpAPI:

  def gatherRoutes(controllers: List[BaseController]) = controllers.flatMap(_.routes)

  def makeControllers() =
    for
      healthController  <- HealthController.makeZIO
      companyController <- CompanyController.makeZIO
    yield List(healthController, companyController)

  val endpointsZIO = makeControllers().map(gatherRoutes)
