package repositories

import javax.inject.{Inject, Singleton}
import models.DataModel
import play.api.libs.json.{JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DataRepository @Inject()(mongo: ReactiveMongoComponent,
                               implicit val ec: ExecutionContext) extends ReactiveRepository[DataModel, BSONObjectID](
  "data",
  mongo.mongoConnector.db,
  DataModel.formats
) {
  def create(data: DataModel): Future[WriteResult] = insert(data)

  def read(id: String): Future[DataModel] = find("_id" -> id) map (_.head)

  def update(data: DataModel): Future[DataModel] = findAndUpdate(
    Json.obj("_id" -> data._id),
    Json.toJson(data).as[JsObject]
  ).map(_ => data)

  def delete(id: String): Future[WriteResult] = remove("_id" -> id)
}
