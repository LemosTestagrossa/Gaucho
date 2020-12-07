CREATE KEYSPACE IF NOT EXISTS eventstore
WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 3};

CREATE TABLE IF NOT EXISTS eventstore.events (
    partition_id bigint,
    seq_nr bigint,
    id text,
    event text,
    PRIMARY KEY ((partition_id, id), seq_nr)
);
CREATE TABLE IF NOT EXISTS eventstore.snapshots (
    partition_id bigint,
    seq_nr bigint,
    id text,
    event text,
    PRIMARY KEY ((partition_id, id), seq_nr)
);