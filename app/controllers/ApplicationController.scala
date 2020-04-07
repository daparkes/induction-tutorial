package controllers

import com.google.inject.Inject
import play.api.mvc.{BaseController, ControllerComponents}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def index = TODO

  def create = TODO

  def read(id: String) = TODO

  def update(id: String) = TODO

  def delete(id: String) = TODO
}
