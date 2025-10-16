package com.eshop.auth.config

import pureconfig.ConfigReader

final case class AppConfig(
    emberConfig: EmberConfig,
    mongoConfig: MongoConfig,
    redisConfig: RedisConfig,
    smtpConfig: SmtpConfig,
) derives ConfigReader
