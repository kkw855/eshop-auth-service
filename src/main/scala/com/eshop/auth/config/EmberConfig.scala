package com.eshop.auth.config

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

import com.comcast.ip4s.{Ipv4Address, Port}

import com.eshop.auth.config.givens

final case class EmberConfig(ipAddress: Ipv4Address, port: Port) derives ConfigReader

object EmberConfig extends givens {
  given ipAddressReader: ConfigReader[Ipv4Address] = ConfigReader[String].emap { ipv4String =>
    Ipv4Address
      .fromString(ipv4String)
      .toRight(
        CannotConvert(ipv4String, Ipv4Address.getClass.toString, s"Invalid ip address: $ipv4String")
      )
  }
}
