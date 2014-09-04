package model

case class Commit(author: CommitAuthor,
              committer: CommitCommitter,
              message: String,
              tree: Tree,
              url: String,
              commentCount: Int)
