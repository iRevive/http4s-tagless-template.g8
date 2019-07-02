FROM eed3si9n/sbt

USER root
RUN set -x && apk --update add --no-cache --virtual docker

USER 1001

WORKDIR /app

ADD build.sbt /app
ADD project /app/project

RUN sbt update

WORKDIR /opt/workspace