# Maven build container

FROM maven:3.6.3-openjdk-11-slim AS maven_build

COPY pom.xml /tmp/

COPY src /tmp/src/

WORKDIR /tmp/

RUN mvn package

#pull base image

FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-alpine-slim

COPY hooks /tmp/hooks/
COPY wait-for /tmp/

RUN apk --no-cache --update add git

# resolving CVE-2019-14697
RUN apk upgrade musl

#maintainer
MAINTAINER ttran@pingidentity.com
#expose port 8080
EXPOSE 8084

CMD cd /tmp/ && ./hooks/start-service.sh

COPY --from=maven_build /tmp/target/cdr-au-sample-consentapp-0.0.1-SNAPSHOT.jar /tmp/app.war
