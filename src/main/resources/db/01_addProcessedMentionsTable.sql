create table processed_mentions (
  tweet_id varchar(50) not null,
  processed_timestamp timestamp not null,
  primary key (tweet_id)
);