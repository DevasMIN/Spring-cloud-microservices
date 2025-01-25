# Logistics System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0-blue.svg)](https://spring.io/projects/spring-cloud)
[![Gradle](https://img.shields.io/badge/Gradle-7.5+-lightgrey.svg)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Kafka](https://img.shields.io/badge/Kafka-7.3.0-black.svg)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-latest-blue.svg)](https://www.docker.com/)

Система автоматизации корпоративной логистической службы для обработки и доставки заказов интернет-магазина.

## О проекте

Система автоматизирует полный цикл обработки заказа от момента его создания до доставки клиенту. Основные этапы обработки:
1. Регистрация заказа
2. Проверка и осуществление оплаты
3. Проверка наличия товаров на складе и комплектация
4. Организация доставки

### Особенности реализации
- Микросервисная архитектура
- Асинхронное взаимодействие между сервисами через Apache Kafka
- Распределенные транзакции с использованием паттерна SAGA (хореография)
- Отказоустойчивость на каждом этапе обработки заказа
- JWT-based аутентификация
- API Gateway для маршрутизации и балансировки нагрузки

## Технологический стек

### Основные технологии
- Java 17
- Spring Boot 3.2.0
- Spring Cloud 2023.0
  - Spring Cloud Gateway
  - Spring Cloud Netflix Eureka
  - Spring Cloud Config
  - Spring Cloud OpenFeign
- Spring Security + JWT
- Spring Data JPA
- Spring for Apache Kafka
- Gradle 7.5+

### База данных и кэширование
- PostgreSQL 14
- Redis 7.0

### Очереди сообщений
- Apache Kafka (Confluent Platform 7.3.0)
- Zookeeper

### Мониторинг и документация
- Spring Boot Actuator
- SpringDoc OpenAPI 2.2.0 (Swagger UI доступен для сервисов auth, payment, order, inventory, delivery)

### Тестирование
- JUnit 5
- Testcontainers
- WireMock
- Mockito
- AssertJ

### Инфраструктура
- Docker
- Docker Compose

## Архитектура проекта

### Бизнес-сервисы
- **Order Service** - управление заказами, отслеживание статусов
- **Payment Service** - обработка платежей
- **Delivery Service** - управление доставкой
- **Inventory Service** - управление складом и товарами

### Системные сервисы
- **Auth Service** - аутентификация и авторизация
- **API Gateway** - единая точка входа, маршрутизация
- **Config Service** - централизованная конфигурация
- **Discovery Service** - обнаружение сервисов (Eureka)

## Предварительные требования

- JDK 17
- Docker и Docker Compose
- Gradle 7.5+
- Минимум 8GB RAM для запуска всех сервисов

## Начало работы

1. Клонируйте репозиторий:
   ```bash
   git clone <repository-url>
   ```

2. Запустите инфраструктурные сервисы:
   ```bash
   docker-compose up -d
   ```
   
   Это запустит:
   - PostgreSQL (порт 5432)
   - Redis (порт 6379)
   - Zookeeper (порт 2181)
   - Kafka (порты 9092, 29092)

3. Порядок запуска сервисов:
   1. Discovery Service (Eureka) - порт 8761
   2. Config Service
   3. Gateway Service
   4. Auth Service
   5. Остальные сервисы (order, payment, delivery, inventory)

4. При первом запуске автоматически создается администратор системы со следующими учетными данными:
   ```json
   {
       "username": "defaultAdmin",
       "password": "defaultPassword"
   }
   ```
   Рекомендуется сменить пароль после первого входа в систему.

## Мониторинг и управление

- Eureka Dashboard: http://localhost:8761
- Swagger UI доступен для каждого сервиса: http://localhost:{port}/swagger-ui.html
- Actuator endpoints: http://localhost:{port}/actuator

## Статусы заказа
- `REGISTERED` - заказ зарегистрирован
- `PAID` - заказ оплачен
- `PAYMENT_FAILED` - ошибка оплаты
- `INVENTED` - заказ укомплектован
- `INVENTMENT_FAILED` - ошибка комплектации
- `DELIVERED` - заказ доставлен
- `DELIVERY_FAILED` - ошибка доставки
- `UNEXPECTED_FAILURE` - непредвиденная ошибка

## Разработка

### Структура модулей
- `common-dto` - общий модуль, содержащий:
  - DTO классы для обмена данными между сервисами
  - Общие утилиты и константы
  - Базовые классы для обработки ошибок
  - Общие конфигурации
  
Все микросервисы зависят от этого модуля, что обеспечивает согласованность данных и переиспользование кода.

### Работа с базой данных
Каждый сервис имеет свою независимую базу данных в PostgreSQL, что обеспечивает изоляцию данных и независимость сервисов друг от друга.

### Асинхронное взаимодействие
Взаимодействие между сервисами происходит асинхронно через Kafka, за исключением обновления статуса заказа, которое выполняется синхронно через REST API.

## Тестирование

```bash
# Запуск модульных тестов
./gradlew test

# Запуск интеграционных тестов
./gradlew integration-test:test
```

## В планах
- Реализация мониторинга и логирования (ELK стэк)
- Реализация полноценного CI\CD
- Sleuth\zipkin
