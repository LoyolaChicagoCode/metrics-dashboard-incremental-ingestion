package lib

import com.google.gson.reflect.TypeToken
import org.eclipse.egit.github.core.client.IGitHubConstants._
import org.eclipse.egit.github.core.client.PagedRequest._
import org.eclipse.egit.github.core.client.{PagedRequest, PageIterator}
import org.eclipse.egit.github.core.{RepositoryCommit, IRepositoryIdProvider}
import org.eclipse.egit.github.core.service.CommitService
import org.joda.time.DateTime

// "pimp my library" pattern
object GitHubTimeUtils {

  implicit class CommitServiceImprovements(val service: CommitService) {

    /**
     * Get all commits in given repository beginning at an optional commit SHA-1
     * and affecting an optional path based on since and until (not available in egit)
     *
     * @param repository the repo
     * @param sha the sha to start from
     * @param path the file to get
     * @param since commits since this time
     * @param until commits up until this time
     * @return the commits
     */
    def getCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: DateTime, until: DateTime) = {

      val iterator = service.pageCommits(repository, sha, path, since, until, PAGE_SIZE)

      val elements = new java.util.ArrayList[RepositoryCommit]

      var current = 0
      while (iterator.hasNext) {
        val it = System.currentTimeMillis()
        elements.addAll(iterator.next)
        val it2 = System.currentTimeMillis()
        current = current + 1
        println("current: " + current)
        println("time: " + (it2 - it))
      }

      elements
    }

    /**
     * Page commits in given repository
     *
     * @param repository the repo
     * @param sha the sha to start from
     * @param path the file to get
     * @param since commits since this time
     * @param until commits up until this time
     * @param size the number of commits per page
     * @return the commits
     */
    def pageCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: DateTime,
                    until: DateTime, size: Int): PageIterator[RepositoryCommit] = {

      val id: String = repository.generateId()
      val uri: java.lang.StringBuilder = new java.lang.StringBuilder(SEGMENT_REPOS)
      uri.append('/').append(id)
      uri.append(SEGMENT_COMMITS)
      val request: PagedRequest[RepositoryCommit] = new PagedRequest[RepositoryCommit](PAGE_FIRST, size)
      request.setUri(uri)
      request.setType(new TypeToken[java.util.List[RepositoryCommit]] {}.getType)
      if (sha != null || path != null || since != null) {
        val params: java.util.Map[String, String] = new java.util.HashMap[String, String]
        if (sha != null) params.put("sha", sha)
        if (path != null) params.put("path", path)
        if (since != null) params.put("since", since.toString)
        if (until != null) params.put("until", until.toString)
        request.setParams(params)
      }
      new PageIterator[RepositoryCommit](request, service.getClient)
    }
  }
}
