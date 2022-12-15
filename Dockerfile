FROM azul/zulu-openjdk-alpine:17
ENV VERSION=0.0.1
ENV TOKEN=$TOKEN

COPY /target/rock-paper-scissors-lizard-spock-bot-$VERSION.jar /rock-paper-scissors-lizard-spock-bot-$VERSION.jar
CMD echo $TOKEN
CMD java -Djava.security.egd=file:/dev/./urandom -jar /rock-paper-scissors-lizard-spock-bot -$VERSION.jar $TOKEN
