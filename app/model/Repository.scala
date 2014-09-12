package model

import org.joda.time.DateTime

case class Repository(_id: Long,
                      full_name: String,
                      last_update: DateTime)
