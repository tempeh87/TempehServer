package server.tempeh.model

import server.tempeh.category.DataSource

case class Episode(title: String, url: String, transcript: String, audioSource: String, dataSource: DataSource)
