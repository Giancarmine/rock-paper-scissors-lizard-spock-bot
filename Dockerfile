FROM azul/zulu-openjdk-alpine:17
ENV VERSION=all
ENV TOKEN=$TOKEN

EXPOSE 8080

CMD ./gradlew build
COPY /build/libs/rock-paper-scissors-lizard-spock-bot-$VERSION.jar /rock-paper-scissors-lizard-spock-bot-$VERSION.jar
CMD echo $TOKEN
CMD java -jar /rock-paper-scissors-lizard-spock-bot-$VERSION.jar $TOKEN
