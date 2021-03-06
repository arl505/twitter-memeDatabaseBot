create table user_memes (
  user_id varchar(50) not null,
  description varchar(500),
  sequence_number bigint unsigned,
  twitter_media_url varchar(100),
  is_gif boolean,
  primary key (user_id, description, sequence_number)
);

create table sequence_number (
  sequence_number bigint unsigned auto_increment,
  primary key (sequence_number)
);