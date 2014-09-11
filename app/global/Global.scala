package global

import java.util

import com.google.gson.reflect.TypeToken
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoURI
import org.eclipse.egit.github.core.client.IGitHubConstants._
import org.eclipse.egit.github.core.client.PagedRequest._
import org.eclipse.egit.github.core.{RepositoryCommit, IRepositoryIdProvider}
import org.eclipse.egit.github.core.client.{PagedRequest, PageIterator, GitHubClient}
import org.eclipse.egit.github.core.service.CommitService
import org.joda.time.DateTime
import play.api.{Application, GlobalSettings}

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

object Global extends GlobalSettings {

  lazy val database = mongoConnection("heroku_app29365494")
  lazy val githubClient = ghClient


  private def mongoConnection = {
    val uri = MongoClientURI(sys.env("MONGOLAB_URI"))
    MongoClient(uri)
  }

  private def ghClient = {
    val client = new GitHubClient()
    client.setCredentials(sys.env("GITHUB_USERNAME"), sys.env("GITHUB_PASSWORD"))
    client
  }

  override def onStart(app: Application) {
    val client = new GitHubClient()
    client.setCredentials("mdotson", "m0n67McEIZGU")

    val col = database.getCollection("test")
    println(col.insert(MongoDBObject("hello" -> "world")))

    future {
      persistCommitHistory(client, database)
    }
  }



  def persistCommitHistory(client: GitHubClient, db: MongoDB) = {

    val commitService = new CommitService(client)

    /*
    while (true) {

      val commits1 = commitService.getCommits(repo)
      val commits2 = commitService.getCommits(repo, null, null, new DateTime(2014, 7, 11, 12, 30))
      Logger.info("end!")
      Logger.info(commits1.size().toString + " commits1")
      Logger.info(commits2.size().toString + " commits2")
      Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis)
    }
    */
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
      println("id is: " + id)
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
