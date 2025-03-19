package utils

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.{GetSecretValueRequest, GetSecretValueResponse}

object SecretsManagerUtil {
  def getSecret(secretName: String): String = {
    val client                                         = SecretsManagerClient.builder().build()
    val getSecretValueRequest                          = GetSecretValueRequest.builder().secretId(secretName).build()
    val getSecretValueResponse: GetSecretValueResponse = client.getSecretValue(getSecretValueRequest)
    getSecretValueResponse.secretString()
  }
}
