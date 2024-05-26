#docker rmi -f $(docker images -aq)
#docker rm -vf $(docker ps -aq)
docker image prune -f
docker container prune -f
