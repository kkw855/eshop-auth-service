package com.eshop.auth.config

import pureconfig.ConfigReader

// TODO: String => RedisURI
final case class RedisConfig(uri: String) derives ConfigReader
