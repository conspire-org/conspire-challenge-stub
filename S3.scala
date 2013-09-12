package com.goconspire.challenge

import scala.Option
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.XML

import java.net.HttpURLConnection
import java.net.URLEncoder
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.MessageDigest
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import play.api.libs.ws._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import org.apache.commons.codec.binary.Base64

object S3 {
    
  /**
   * Lists the contents of the specified bucket
   */
  def list(accessKeyId: String, secretAccessKey: String, bucket: String):
      Future[Seq[String]] = {
    getRequest(accessKeyId, secretAccessKey, bucket, "").map { response =>
      checkResponse("", response)
      val root = XML.loadString(response.body)
      (root \ "Contents" \ "Key").map { node =>
        node.text
      }
    }
  }
  
  /**
   * Reads the contents of the specified object in the specified bucket
   */
  def get(accessKeyId: String, secretAccessKey: String, bucket: String,
      objectKey: String): Future[String] = {
    getRequest(accessKeyId, secretAccessKey, bucket, objectKey).map { response =>
      checkResponse(objectKey, response)
      response.body
    }
  }
  
  /**
   * Makes an authenticated HTTP GET request to S3
   */
  val awsDateFormatter = DateTimeFormat.forPattern("EEE, d MMM yyyy HH:mm:ss Z")
  private def getRequest(accessKeyId: String, secretAccessKey: String, bucket: String,
      extension: String): Future[Response] = {
    
    val timestamp = awsDateFormatter.print(new DateTime())
    
    val toSign = s"GET\n\n\n${timestamp}\n/${bucket}/${extension}"
    val sig = calculateRFC2104HMAC(toSign, secretAccessKey)

    WS.url(s"http://${bucket}.s3.amazonaws.com/${extension}").withHeaders(
      ("Authorization", s"AWS ${accessKeyId}:${sig}"),
      ("Date", timestamp)
    ).get
  }
  
  /**
   * Computes the signature for the Authorization header
   */
  val HmacSha1Algorithm = "HmacSHA1"
  private def calculateRFC2104HMAC(data: String, key: String): String = {
  
    // get an hmac_sha1 key from the raw key bytes
    val signingKey = new SecretKeySpec(key.getBytes, HmacSha1Algorithm)

    // get an hmac_sha1 Mac instance and initialize with the signing key
    val mac = Mac.getInstance(HmacSha1Algorithm)
    mac.init(signingKey)

    // compute the hmac on input data bytes
    val rawHmac = mac.doFinal(data.getBytes)

    // base64-encode the hmac
    new String(Base64.encodeBase64(rawHmac))
  }
  
  /**
   * Checks an S3 response for unexpected statuses and throws Exceptions as appropriate
   */
  private def checkResponse(extension: String, response: Response) = {
    response.status match {
      case HttpURLConnection.HTTP_NOT_FOUND => 
        throw new FileNotFoundException("Request to " + extension + " failed with status 404")
      case HttpURLConnection.HTTP_FORBIDDEN =>
        throw new Exception("Signature for request to " + extension +
            " did not match request. Response body:\n" + response.body)
      case HttpURLConnection.HTTP_OK =>
        // Ok
      case _ =>
        throw new Exception("Request to " + extension + " failed with status " +
            response.status + "/" + response.statusText +
            ". Response body:\n" + response.body)
    }
  }
}
