package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import zio.config.derivation.name

final case class Company(
    id: Long,
    name: String,
    url: String,
    slug: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List.empty
)

object Company:
  given codec: JsonCodec[Company]    = DeriveJsonCodec.gen[Company]
  def makeSlug(name: String): String = name.toLowerCase
    .replace(" +", "-")
    .split(" ")
    .map(_.toLowerCase())
    .mkString("-")
