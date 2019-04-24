create database hamster;
use hamster;
create user 'test'@'localhost' identified by '9996';
grant all on hamster.* to 'test'@'localhost';

CREATE TABLE user_data (
     id MEDIUMINT NOT NULL AUTO_INCREMENT,
     fir_data TEXT NOT NULL,
     PRIMARY KEY (id)
);

#INSERT INTO user_data (fir_data) VALUES ("hello world");
#SELECT * FROM user_data;
#truncate table user_data;
