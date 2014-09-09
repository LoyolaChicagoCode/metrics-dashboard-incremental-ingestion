import java.util.concurrent.TimeUnit

import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.{CommitService, RepositoryService}
import play.api._
import scala.concurrent.future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.Duration

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    val client = new GitHubClient()
    client.setCredentials("mdotson", "m0n67McEIZGU")
    future {
      while (true) {
        Logger.info("start!")
        val service = new RepositoryService(client)
        val stuff = service.getRepository("playframework", "playframework")
        new CommitService(client).getCommits(stuff)
        Logger.info("end!")
        Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis)
      }
    }
  }
}
