package com.eshop.auth.config

import pureconfig.ConfigReader

final case class RedisConfig(uri: String) derives ConfigReader
