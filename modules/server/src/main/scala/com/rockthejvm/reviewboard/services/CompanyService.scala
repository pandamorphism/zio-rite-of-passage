package com.rockthejvm.reviewboard.services
import zio.*
import com.rockthejvm.reviewboard.domain.data.Company
import collection.mutable
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository

trait CompanyService:
  def create(req: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
end CompanyService

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService:
  override def create(req: CreateCompanyRequest): Task[Company] =
    repo.create(req.toCompany(-1L))
  override def getAll: Task[List[Company]]                      = repo.getAll
  override def getById(id: Long): Task[Option[Company]]         = repo.getById(id)
  override def getBySlug(slug: String): Task[Option[Company]]   = repo.getBySlug(slug)
end CompanyServiceLive
object CompanyServiceLive:
  val layer = ZLayer {
    for repo <- ZIO.service[CompanyRepository]
    yield new CompanyServiceLive(repo)
  }
