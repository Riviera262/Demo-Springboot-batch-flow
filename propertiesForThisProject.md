spring.application.name=Transactions-Management

# Oracle connection
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/XEPDB1
spring.datasource.username=
spring.datasource.password=
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# JPA configure
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.OracleDialect

# Batch Insert
app.upload.dir=./uploads
spring.jpa.properties.hibernate.jdbc.batch_size=5000
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.batch_versioned_data=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true

# Files upload limit
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=600MB

# Secret Key JWT
app.jwt.secret=transactionsmanagement.app.jwtSecret=MySecretKeyForJWTTokenSigningMustBeAtLeast256BitsLongForHS256Algorithm
# Expired time of JWT (24h)
app.jwt.expiration-ms=86400000

# Schedule (30m)
app.scheduler.recovery-delay=1800000

# Logging
logging.level.org.example.transactionsmanagement=INFO
logging.level.org.hibernate.SQL=OFF
logging.level.org.hibernate.type.descriptor.sql=OFF

server.port=8081
logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping=TRACE
