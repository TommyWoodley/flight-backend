provider "aws" {
  region = "us-west-2"
}

resource "aws_secretsmanager_secret" "flight_labs_api" {
  name        = "flight_labs_api"
  description = "API key for Flight Labs"
}