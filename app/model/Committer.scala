package model

case class Committer(login: String,
                     id: Int,
                     avatarUrl: String,
                     gravatarId: String,
                     url: String,
                     htmlUrl: String,
                     followersUrl: String,
                     followingUrl: String,
                     gistsUrl: String,
                     starredUrl: String,
                     subscriptionsUrl: String,
                     organizationsUrl: String,
                     reposUrl: String,
                     eventsUrl: String,
                     receivedEventsUrl: String,
                     `type`: String,
                     siteAdmin: Boolean)
