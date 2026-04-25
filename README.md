# FoodShop Backend

Backend cua du an FoodShop duoc xay dung bang Spring Boot, cung cap REST API cho cac chuc nang xac thuc, quan ly san pham, gio hang, don hang, ma giam gia, dia chi va thanh toan VNPay.

## Cong nghe su dung

- Java 19
- Spring Boot 3.3.2
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- JWT
- MapStruct
- Lombok

## Cau truc chinh

```text
backend/foodshop/
|- src/main/java/com/foodshop
|- src/main/resources
|  |- application.properties
|  \- db/migration
|- pom.xml
\- mvnw.cmd
```

## Yeu cau moi truong

Can cai san:

- Java 19
- Maven 3.9+ hoac dung Maven Wrapper
- MySQL 8+

## Cau hinh moi truong

Backend dang doc cau hinh tu `application.properties` thong qua bien moi truong.

Can chuan bi cac bien sau:

```env
mysql_url=jdbc:mysql://localhost:3306/foodshop
mysql_username=root
mysql_password=your_password

jwt_secret=your_jwt_secret
jwt_access_expiration=36000000
jwt_refresh_expiration=360000000

VNPAY_TEST=your_vnpay_tmn_code
VNPAY_SECRET_KEY=your_vnpay_secret

cloudinary_cloud_name=your_cloud_name
cloudinary_api_key=your_api_key
cloudinary_api_secret=your_api_secret
```

## Tao database

Tao database truoc trong MySQL:

```sql
CREATE DATABASE foodshop;
```

Khong can tu tao bang thu cong neu chay bang Flyway migration.

## Cach chay du an

Di chuyen vao thu muc backend:

```bash
cd backend/foodshop
```

Chay ung dung bang Maven Wrapper:

### Windows

```bash
mvnw.cmd spring-boot:run
```

### Linux / macOS

```bash
./mvnw spring-boot:run
```

Sau khi chay thanh cong, backend mac dinh hoat dong tai:

```text
http://localhost:8080
```

## Database migration

Du an su dung Flyway de quan ly schema database.

Thu muc migration:

```text
src/main/resources/db/migration
```

Khi ung dung khoi dong, Flyway se tu chay cac migration can thiet.

## Cau hinh hien tai

Mot so cau hinh dang chu y:

- `spring.jpa.hibernate.ddl-auto=none`
- `spring.flyway.enabled=true`
- `spring.flyway.baseline-on-migrate=true`

Dieu nay co nghia la database nen duoc quan ly bang migration, khong nen sua schema thu cong trong code production.

## Mot so API tieu bieu

### Auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh-token`

### Product
- `GET /api/v1/products`
- `GET /api/v1/products/search`
- `GET /api/v1/products/{id}`

### Order
- `POST /api/v1/orders`
- `GET /api/v1/orders/my`
- `GET /api/v1/orders/{id}`

### Admin
- `GET /api/v1/admin/products`
- `GET /api/v1/admin/orders`
- `GET /api/v1/admin/users`
- `GET /api/v1/admin/discounts`

## Luu y

- Khong nen commit thu muc `logs/`
- Nen tach bien moi truong theo tung moi truong `dev`, `test`, `prod`
- Neu loi ket noi database, kiem tra lai `mysql_url`, `mysql_username`, `mysql_password`

## Kiem tra nhanh

Sau khi backend chay, co the kiem tra bang endpoint:

```text
GET /api/v1/auth/ping
```
