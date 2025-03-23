FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.10.11_2.13.16

COPY . .

RUN sbt compile

EXPOSE 9000

CMD ["sbt", "run"]