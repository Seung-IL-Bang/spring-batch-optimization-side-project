FROM mysql:8.0
ARG MYSQL_ROOT_PASSWORD
ENV MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
COPY ./mysql-files/ /var/lib/mysql-files/
COPY ./mysql-init-sql/init-commerce.sql /docker-entrypoint-initdb.d/init.sql