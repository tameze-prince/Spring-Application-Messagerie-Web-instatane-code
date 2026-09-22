.PHONY: dev-up dev-down tools-up tools-down build run test clean

dev-up:
	docker compose -f docker-compose.dev.yml up -d

dev-down:
	docker compose -f docker-compose.dev.yml down -v

tools-up:
	docker compose -f docker-compose.tools.yml up -d

tools-down:
	docker compose -f docker-compose.tools.yml down -v

build:
	./mvnw clean package -DskipTests

run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

test:
	./mvnw clean test

clean:
	./mvnw clean
