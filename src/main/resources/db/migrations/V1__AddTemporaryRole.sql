CREATE TABLE temporary_role
(
  user_id  bigint NOT NULL,
  role_id  bigint NOT NULL,
  guild_id bigint NOT NULL,
  until    bigint NOT NULL,
  PRIMARY KEY (user_id, role_id)
)