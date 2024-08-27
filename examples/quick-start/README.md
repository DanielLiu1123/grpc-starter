## Run

```shell
docker rm -f mysqldb && docker run -id --name mysqldb -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:latest && sleep 10 && docker exec -i mysqldb mysql -uroot -proot -e "CREATE DATABASE testdb; USE testdb; CREATE TABLE pet (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), status INT);"
```