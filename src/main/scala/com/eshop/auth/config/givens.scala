package com.eshop.auth.config

import com.comcast.ip4s.Port
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

trait givens {
  given portReader: ConfigReader[Port] = ConfigReader[Int].emap { portInt =>
    Port
      .fromInt(portInt)
      .toRight(
        CannotConvert(portInt.toString, Port.getClass.toString, s"Invalid port: $portInt")
      )
  }
}
