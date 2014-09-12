package globalobj

import java.util.concurrent.TimeUnit

import com.google.gson.reflect.TypeToken
import com.mongodb.casbah.commons.conversions.scala.{RegisterJodaTimeConversionHelpers, RegisterConversionHelpers}
import org.eclipse.egit.github.core.client.IGitHubConstants._
import org.eclipse.egit.github.core.client.PagedRequest._
import org.eclipse.egit.github.core.{RepositoryCommit, IRepositoryIdProvider}
import org.eclipse.egit.github.core.client.{PagedRequest, PageIterator, GitHubClient}
import org.eclipse.egit.github.core.service.CommitService
import org.joda.time.DateTime
import play.api.{Play, Logger, Application, GlobalSettings}
import com.novus.salat._
import com.mongodb.casbah.Imports._
import model.Repository

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration.Duration

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
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()
    ctx.registerClassLoader(Play.classloader(Play.current))

    future {
      persistCommitHistorySince(githubClient, database)
    }
  }

  implicit lazy val ctx = new Context {
    val name = "Custom_Classloader"
  }

  def persistCommitHistorySince(client: GitHubClient, db: MongoDB) = {

    val commitService = new CommitService(client)

    while (true) {

      val currentTime = DateTime.now()

      val repoCollection = database("repositories")

      val allRepos = repoCollection.find().toList

      Logger.info("start!")
      // convert DBObject to Repository
      allRepos map {
        dbObject =>
          val repo = grater[Repository].asObject(dbObject)
          val repoWithTime = Repository(repo._id, repo.full_name, currentTime)
          repoCollection.update(dbObject, grater[Repository].asDBObject(repoWithTime))
      }
      Logger.info("end!")

      blocking(Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis))

      /*
      commitService.getCommits(Repository, null, null, timeSinceOpt, currentTime)
      */

      /*
      val commits1 = commitService.getCommits(repo)
      val commits2 = commitService.getCommits(repo, null, null, new DateTime(2014, 7, 11, 12, 30))
      Logger.info("end!")
      Logger.info(commits1.size().toString + " commits1")
      Logger.info(commits2.size().toString + " commits2")
      Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis)
      */
    }
  }
}

object SinceUtils {

  implicit class CommitServiceImprovements(val s: CommitService) {

    def getCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: Option[DateTime], until: DateTime) = {

      val iterator = s.pageCommits(repository, sha, path, since, until, 100)

      val elements = new java.util.ArrayList[RepositoryCommit]

      while (iterator.hasNext) elements.addAll(iterator.next)

      elements
    }

    def pageCommits(repository: IRepositoryIdProvider, sha: String, path: String, since: Option[DateTime],
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
        since.map(t => params.put("since", t.toString))
        if (until != null) params.put("until", until.toString)
        request.setParams(params)
      }
      new PageIterator[RepositoryCommit](request, s.getClient)
    }
  }

}
