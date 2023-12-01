package com.rockthejvm.reviewboard.http.controllers
import collection.mutable
import zio.*
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import com.rockthejvm.reviewboard.domain.data.Company
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.services.CompanyService

class CompanyController private (service: CompanyService)
    extends BaseController
    with CompanyEndpoints:

  // create
  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogicSuccess { req =>
    service.create(req)
  }

  val getAll: ServerEndpoint[Any, Task] = getAllEndpoint.serverLogicSuccess { _ =>
    service.getAll
  }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogicSuccess { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(service.getById)
      .catchSome { case _: NumberFormatException => service.getBySlug(id) }
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)

end CompanyController

object CompanyController:
  val makeZIO =
    for service <- ZIO.service[CompanyService]
    yield new CompanyController(service)
