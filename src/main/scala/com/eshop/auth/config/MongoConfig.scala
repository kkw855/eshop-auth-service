package com.eshop.auth.config

import pureconfig.ConfigReader

final case class MongoConfig(uri: String/*, host: String, port: Int, database: String, user: String, pass: String*/)
    derives ConfigReader
