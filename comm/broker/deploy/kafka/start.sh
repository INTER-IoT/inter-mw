#!/bin/bash
cd docker
docker-compose kill
docker-compose rm -f
docker-compose up --build -d
