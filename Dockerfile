FROM azul/zulu-openjdk-alpine:17
ENV VERSION=1.0-SNAPSHOT
ENV SUFFIX=-jar-with-dependencies
ENV TOKEN=$TOKEN

COPY /target/rock-paper-scissors-lizard-spock-bot-$VERSION$SUFFIX.jar /rock-paper-scissors-lizard-spock-bot-$VERSION$SUFFIX.jar
CMD echo $TOKEN
CMD java -Djava.security.egd=file:/dev/./urandom -jar /rock-paper-scissors-lizard-spock-bot -$VERSION$SUFFIX.jar $TOKEN
