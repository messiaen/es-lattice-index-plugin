# Disable built-in rules and variables
MAKEFLAGS += --no-builtin-rules
MAKEFLAGS += --no-builtin-variables

ES_VERSION = $(shell grep elasticsearchVersion gradle.properties  | cut -d " " -f 3)
VERSION = $(shell cat VERSION.txt)
IMAGE_VERSION = $(VERSION)-$(ES_VERSION)
NAME = "messiaen/elasticsearch-plug-ph-lat"

default: build_plugin

clean:
	./gradlew clean

build_plugin:
	./gradlew clean assemble

build_image: build_plugin
	docker build --no-cache -t $(NAME):$(IMAGE_VERSION) .
	docker tag $(NAME):$(IMAGE_VERSION) $(NAME):latest

push:
	docker push $(NAME):$(IMAGE_VERSION)

test:
	./gradlew cleanTest test

build:
	./gradlew build

run: stop build_image
	docker-compose up -d

stop:
	docker-compose down --volumes
