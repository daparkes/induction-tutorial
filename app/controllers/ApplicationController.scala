package controllers

import com.google.inject.Inject
import models.DataModel
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import reactivemongo.core.errors.{DatabaseException, DriverException}
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents,
                                      val repo: DataRepository,
                                      implicit val ec: ExecutionContext
                                     ) extends BaseController {
  def index: Action[AnyContent] = Action.async { implicit request =>
    repo.find().map(items => Ok(Json.toJson(items)))
  }

  def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        repo.create(dataModel).map(_ => Created) recover {
          case _: DriverException => InternalServerError(Json.obj(
            "message" -> "Error adding item to Mongo"
          ))
        }
      case JsError(_) => Future(BadRequest)
    }
  }

  def read(id: String): Action[AnyContent] = Action.async { implicit request =>
    repo.read(id).map(item => Ok(Json.toJson(item))) recover {
      case _: DriverException => InternalServerError(Json.obj(
        "message" -> "Error reading from database"
      ))
    }
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[DataModel] match {
      case JsSuccess(dataModel, _) =>
        repo.update(dataModel).map(_ => Accepted(Json.toJson(dataModel))) recover {
          case _: DriverException => InternalServerError(Json.obj(
            "message" -> "Error updating database"
          ))
        }
      case JsError(_) => Future(BadRequest)
    }
  }

  def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
    repo.delete(id).map(_ => Accepted) recover {
      case _: DriverException => InternalServerError(Json.obj(
        "message" -> "Error deleting from database"
      ))
    }
  }
}
