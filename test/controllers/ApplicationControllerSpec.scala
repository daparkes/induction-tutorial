package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.DataModel
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.{Status, Writeable}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import repositories.DataRepository
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.commands.{LastError, WriteResult}
import reactivemongo.core.errors.GenericDriverException

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val mockDataRepository: DataRepository = mock[DataRepository]

  object TestApplicationController extends ApplicationController(
    controllerComponents, mockDataRepository, executionContext
  )

  implicit val system: ActorSystem = ActorSystem("Sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

  "ApplicationController .index" should {

    when(mockDataRepository.find(any())(any()))
      .thenReturn(Future(List(dataModel)))
    val result = TestApplicationController.index()(FakeRequest())

    "return the correct JSON" in {
      await(jsonBodyOf(result)) shouldBe Json.arr(Json.toJson(dataModel))
    }

    "return OK" in {
      status(result) shouldBe Status.OK
    }
  }

  "ApplicationController .create()" when {
    "the json body is valid " should {
      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      val writeResult: WriteResult = LastError(
        ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None
      )

      when(mockDataRepository.create(any()))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

      "return CREATED" in {
        status(result) shouldBe Status.CREATED
      }
    }

    "the json body is not valid" should {
      val invalidJsonBody: JsObject = Json.obj(
      )

      val writeResult: WriteResult = LastError(
        ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None
      )

      when(mockDataRepository.create(any()))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.create()(FakeRequest().withBody(invalidJsonBody))

      "return an error " in {
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "the mongo data creation failed" should {

      val jsonBody = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      when(mockDataRepository.create(any()))
        .thenReturn(Future.failed(GenericDriverException("Error")))

      "return an error" in {
        val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR

        await(bodyOf(result)) shouldBe Json.obj("message" -> "Error adding item to Mongo").toString()
      }
    }
  }

  "ApplicationController .read()" should {
    when(mockDataRepository.read(any()))
      .thenReturn(Future(dataModel))
    val result = TestApplicationController.read(dataModel._id)(FakeRequest())
    "return an OK status" in {
      status(result) shouldBe Status.OK
    }
  }

  "ApplicationController .update()" when {

    when(mockDataRepository.update(any()))
      .thenReturn(Future(dataModel))

    "the json body is valid " should {
      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      "return a valid JSON body" in {
        val result = TestApplicationController.update(dataModel._id)(FakeRequest().withBody(jsonBody))
        await(jsonBodyOf(result)) shouldBe jsonBody
      }
    }

    "return an error if the JSON is invalid" in {
      val invalidJsonBody: JsObject = Json.obj()
      val result = TestApplicationController.update(dataModel._id)(FakeRequest().withBody(invalidJsonBody))
      status(result) shouldBe Status.BAD_REQUEST
    }
  }

  "ApplicationController .delete()" should {
    "return Accepted" in {
      val writeResult: WriteResult = LastError(
        ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None
      )

      when(mockDataRepository.delete(any()))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.delete(any())(FakeRequest())
      status(result) shouldBe Status.ACCEPTED
    }

    "return an error" in {
      when(mockDataRepository.delete(any()))
        .thenReturn(Future.failed(GenericDriverException("Error")))
        val result = TestApplicationController.delete(any())(FakeRequest())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        await(bodyOf(result)) shouldBe Json.obj("message" -> "Error deleting from database").toString()
    }
  }
}
