docker-compose -f assets/docker-compose/docker-compose-cassandra.yml down -v
docker-compose -f assets/docker-compose/docker-compose-scylla.yml down -v
docker-compose -f assets/docker-compose/docker-compose-monitoring.yml down -v
docker-compose -f assets/docker-compose/docker-compose-kafka.yml down -v

# docker-compose -f assets/docker-compose/docker-compose-cassandra.yml up -d
docker-compose -f assets/docker-compose/docker-compose-scylla.yml up -d
docker-compose -f assets/docker-compose/docker-compose-monitoring.yml build grafana
docker-compose -f assets/docker-compose/docker-compose-monitoring.yml up -d
docker-compose -f assets/docker-compose/docker-compose-kafka.yml up -d


checkCassandra() {
  docker exec cassandra cqlsh -e 'describe tables' > /dev/null 2>&1
}

while ! checkCassandra; do
    sleep 1
done

sh assets/scripts/cassandra/setup_cassandra.sh
