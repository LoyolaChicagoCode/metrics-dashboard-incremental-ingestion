package globalobj

import com.mongodb.casbah.Imports._
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.{RepositoryService, CommitService}

object RemoteConnections {

  lazy val githubClient = RemoteConnections.ghClient
  lazy val commitService = new CommitService(githubClient)
  lazy val repositoryService = new RepositoryService(githubClient)

  lazy val database = RemoteConnections.mongoConnection
  lazy val repositoriesCollection = database("repositories")
  lazy val commitsCollection = database("commits")

  /**
   * Connects to the database via the uri provided in the environment variable.
   *
   * @return the connection to the database
   */
  private[globalobj] def mongoConnection = {
    val mongolabVar = sys.env("MONGOLAB_URI")
    val uri = MongoClientURI(mongolabVar)
    val client = MongoClient(uri)
    client(mongolabVar.substring(mongolabVar.lastIndexOf('/') + 1))
  }

  /**
   * Creates the GitHub client and sets the credentials
   *
   * @return the GH client with the credentials
   */
  private[globalobj] def ghClient = {
    val client = new GitHubClient()
    client.setCredentials(sys.env("GITHUB_USERNAME"), sys.env("GITHUB_PASSWORD"))
    client
  }
}
