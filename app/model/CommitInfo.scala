package model

case class CommitInfo(sha: String,
                   commit: Commit,
                       url: String,
                       htmlUrl: String,
                       commentsUrl: String,
                       author: String,
                       committer: String,
                       parents: List[Parent])
