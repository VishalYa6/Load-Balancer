# Dispatch Load Balancer

A Spring Boot service that accepts delivery orders and vehicles, then produces a dispatch plan by assigning orders to the nearest available vehicle that still has capacity.

**Features**
- Store delivery orders and vehicles in an in-memory H2 database.
- Generate a dispatch plan based on vehicle capacity and distance.
- Input validation with clear error responses.

**Tech Stack**
- Java 21+
- Spring Boot 3.3.x (Web, Validation, Data JPA)
- H2 Database
- Maven Wrapper

**Quick Start**
1. Build

```bash
./mvnw -DskipTests compile
```

2. Run

```bash
./mvnw spring-boot:run
```

The service starts on `http://localhost:8080`.

**H2 Console**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:dispatchdb`
- User: `sa`
- Password: (empty)

**API**
Base path: `/api/dispatch`

1. Save Orders

`POST /api/dispatch/orders`

Request body:
```json
{
  "orders": [
    {
      "orderId": "O-1001",
      "latitude": 12.9716,
      "longitude": 77.5946,
      "address": "MG Road, Bengaluru",
      "packageWeight": 5.5,
      "priority": "HIGH"
    }
  ]
}
```

Response:
```json
{
  "message": "Delivery orders accepted.",
  "status": "success"
}
```

2. Save Vehicles

`POST /api/dispatch/vehicles`

Request body:
```json
{
  "vehicles": [
    {
      "vehicleId": "V-10",
      "capacity": 25.0,
      "currentLatitude": 12.9667,
      "currentLongitude": 77.5667,
      "currentAddress": "Majestic, Bengaluru"
    }
  ]
}
```

Response:
```json
{
  "message": "Vehicle details accepted.",
  "status": "success"
}
```

3. Generate Dispatch Plan

`GET /api/dispatch/plan`

Response shape:
```json
{
  "dispatchPlan": [
    {
      "vehicleId": "V-10",
      "totalLoad": 5.5,
      "totalDistance": 4.2,
      "assignedOrders": [
        {
          "orderId": "O-1001",
          "latitude": 12.9716,
          "longitude": 77.5946,
          "address": "MG Road, Bengaluru",
          "packageWeight": 5.5,
          "priority": "HIGH"
        }
      ]
    }
  ]
}
```

**Validation and Error Responses**
- Validation failures return `400` with a field-to-message map.
- If no vehicles exist, returns `404` with an error message.
- If an order cannot be assigned due to capacity, returns `400` with an error message.

Example error:
```json
{
  "status": "error",
  "message": "Order O-1001 cannot be assigned due to capacity limits"
}
```

**Tests**
```bash
./mvnw test
```