import java.util
import java.util.concurrent.TimeUnit

import com.google.gson.reflect.TypeToken
import org.eclipse.egit.github.core.{RepositoryCommit, IRepositoryIdProvider}
import org.eclipse.egit.github.core.client._
import org.eclipse.egit.github.core.client.IGitHubConstants._
import org.eclipse.egit.github.core.client.PagedRequest._
import org.eclipse.egit.github.core.service.{CommitService, RepositoryService}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api._
import scala.collection.mutable.HashMap
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import com.mongodb.casbah.Imports._

import scala.concurrent.duration.Duration

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    val client = new GitHubClient()
    client.setCredentials("mdotson", "m0n67McEIZGU")

    import SinceUtils.CommitServiceImprovements

    test

    future {
      while (true) {
        Logger.info("start!")
        val service = new RepositoryService(client)
        val stuff = service.getRepository("mdotson", "metrics-dashboard")
        val commitService = new CommitService(client)
        val commits1 = commitService.getCommits(stuff)
        val commits2 = commitService.getCommits(stuff, null, null, new DateTime(2014, 7, 11, 12, 30))
        Logger.info("end!")
        Logger.info(commits1.size().toString + " commits1")
        Logger.info(commits2.size().toString + " commits2")
        Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis)
      }
    }
  }

  def test = {
    val mongoClient = MongoClient("ds035290.mongolab.com/heroku_app29365494", 35290)
    val db = mongoClient("test")
    println("names: " + db.collectionNames())
  }
}

object SinceUtils {

  implicit class CommitServiceImprovements(val s: CommitService) {

    def getCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: DateTime) = {

      val iterator = s.pageCommits(repository, sha, path, since, 100)

      val elements = new util.ArrayList[RepositoryCommit]

      while (iterator.hasNext) elements.addAll(iterator.next)

      elements
    }

    def pageCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: DateTime,
                             size: Int): PageIterator[RepositoryCommit] = {
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
        request.setParams(params)
      }
      new PageIterator[RepositoryCommit](request, s.getClient)
    }
  }

}