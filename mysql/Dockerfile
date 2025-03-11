FROM mysql:latest
COPY ./mysql-files/ /var/lib/mysql-files/
COPY ./mysql-init-sql/init-commerce.sql /docker-entrypoint-initdb.d/init.sql