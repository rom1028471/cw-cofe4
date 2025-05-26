# Кофейня — Spring Boot приложение

## Описание
Приложение для управления кофейней: регистрация, аутентификация, корзина, профиль, админка, роли, товары, категории.

## Запуск локально
1. Убедитесь, что установлен PostgreSQL и создана база `coffeeshop`.
2. В файле `src/main/resources/application.yml` укажите свои данные для подключения к БД.
3. Соберите проект:
   ```
   .\gradlew.bat build
   ```
4. Запустите приложение:
Сначала посмотрите содержимое папки build/libs:

powershell
ls build/libs
   ```
   java -jar build/libs/coffeeshop-0.0.1-SNAPSHOT.jar
   ```
5. Приложение будет доступно на [http://localhost:8080](http://localhost:8080)

## Запуск в Docker
1. Соберите jar:
   ```
   ./gradlew build
   ```
2. Соберите образ:
   ```
   docker build -t coffeeshop .
   ```
3. Запустите контейнер:
   ```
   docker run -p 8080:8080 --env SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/coffeeshop --env SPRING_DATASOURCE_USERNAME=postgres --env SPRING_DATASOURCE_PASSWORD=postgres coffeeshop
   ```

## Тестовые данные
- Админ: admin / admin (Basic Auth)
- Категории: Кофе, Десерты
- Товары: Эспрессо, Капучино, Чизкейк

## Примеры curl
### Регистрация
```
curl -X POST -d "username=test&email=test@mail.com&password=1234" http://localhost:8080/register
```
### Логин (Basic Auth)
```
curl -u admin:admin http://localhost:8080/profile
```
### Получить главную
```
curl http://localhost:8080/main
```

## Страницы
- `/register` — регистрация
- `/login` — вход
- `/main` — главная
- `/cart` — корзина (USER)
- `/profile` — профиль (USER)
- `/admin` — админка (ADMIN)

## Медиа
Картинки и медиа-файлы кладите в `src/main/resources/static/media/`.

## Схема Базы Данных (генерируется Hibernate на основе сущностей)

Spring Boot с Hibernate (`spring.jpa.hibernate.ddl-auto=update`) автоматически управляет схемой базы данных на основе Java-сущностей в пакете `com.example.coffeeshop.model`.

Основные таблицы и их предполагаемые поля:

*   **`roles`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `name` (VARCHAR, уникальное, не null)
*   **`users`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `username` (VARCHAR, уникальное, не null)
    *   `password` (VARCHAR, не null)
    *   `email` (VARCHAR, уникальное, не null)
    *   `created_at` (TIMESTAMP, не null)
*   **`users_roles`** (таблица связей для User и Role, ManyToMany)
    *   `user_id` (FK к users.id)
    *   `role_id` (FK к roles.id)
*   **`categories`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `name` (VARCHAR, уникальное, не null)
*   **`products`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `name` (VARCHAR, не null)
    *   `description` (TEXT)
    *   `price` (DECIMAL/FLOAT, не null)
    *   `category_id` (FK к categories.id)
    *   `available` (BOOLEAN, не null)
*   **`carts`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `user_id` (FK к users.id, уникальное, не null, OneToOne)
*   **`cart_items`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `product_id` (FK к products.id)
    *   `quantity` (INT, не null)
    *   `cart_id` (FK к carts.id) (связь с корзиной)
*   **`orders`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `user_id` (FK к users.id, не null)
    *   `total_price` (DECIMAL, не null)
    *   `status` (VARCHAR, не null)
    *   `created_at` (TIMESTAMP, не null, не обновляется)
*   **`order_items`**
    *   `id` (PK, BIGINT, автоинкремент)
    *   `product_id` (FK к products.id, не null)
    *   `quantity` (INT, не null)
    *   `price_at_purchase` (DECIMAL, не null)
    *   `order_id` (FK к orders.id)

Файл `src/main/resources/data.sql` используется для начального заполнения этих таблиц данными после их создания/обновления Hibernate.

