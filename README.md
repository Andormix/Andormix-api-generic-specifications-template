 🛠️ Guía de Uso: Plantilla Genérica de Consultas (GenericSpecifications)

 <img width="1789" height="635" alt="image" src="https://github.com/user-attachments/assets/6d4a0415-fc85-4643-bb4b-09344d3eadd7" />


> **Autor:** Eric Torrontera Ruiz

Esta utilería (`GenericSpecifications`) proporciona una abstracción limpia, segura y libre de código repetitivo para construir **consultas dinámicas y filtrados combinados** en Spring Data JPA mediante **JPA Criteria API**.

Permite aplicar filtros dinámicos basados en `@RequestParam` opcionales en una sola línea de código, traduciendo automáticamente los filtros a consultas SQL optimizadas con `JOIN` e instrucciones `WHERE` case-insensitive.

---

## Requisitos Previos

Para que cualquier repositorio de tu proyecto pueda hacer uso de `GenericSpecifications`, la interfaz del repositorio **debe extender `JpaSpecificationExecutor<T>`**:

```java
package com.andormix.swipemarketapi.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends 
        JpaRepository<Order, Long>, 
        JpaSpecificationExecutor<Order> { // <--- OBLIGATORIO
}
```

---

## Métodos Disponibles y Cómo Funcionan

### 1. `isEqualTo(fieldName, value)`
Filtra por igualdad exacta en un campo de la entidad principal. Si el `value` recibido es `null`, el filtro se ignora automáticamente (no rompe la consulta ni aplica restricción).

**Java:**
```java
GenericSpecifications.isEqualTo("status", status)
```
*Equivalente conceptual Java:* `order.getStatus().equals(status)`

**SQL generado:**
```sql
WHERE order.status = 'DELIVERED'
```

---

### 2. `joinIsEqualTo(relationName, fieldName, value)`
Navega a través de una relación JPA (`@ManyToOne`, `@OneToOne`) y aplica una igualdad en un campo de la entidad relacionada. Si el `value` es `null`, el filtro se ignora.

**Firma:** `joinIsEqualTo("nombreAtributoRelacion", "campoEntidadRelacionada", valor)`

**Java:**
```java
GenericSpecifications.joinIsEqualTo("customer", "email", customerEmail)
```

**Explicación del mapeo:**
- `"customer"`: Nombre del atributo de relación dentro de la entidad `Order` (`private User customer;`).
- `"email"`: Nombre de la propiedad dentro de la clase `User` (`private String email;`).
- `customerEmail`: Valor que buscamos.

*Equivalente conceptual Java:* `order.getCustomer().getEmail().equals(customerEmail)`

**SQL generado:**
```sql
INNER JOIN users u ON order.customer_id = u.id
WHERE u.email = 'ejemplo@correo.com'
```

---

### 3. `containsTextInFields(search, fieldNames...)`
Aplica una búsqueda parcial `LIKE %search%` (case-insensitive) sobre uno o varios campos.

Soporta dos variantes de sintaxis:
- **Campos directos:** `"trackingCode"`
- **Campos anidados (vía sintaxis de punto .):** `"product.title"` o `"customer.displayName"`

**Java:**
```java
GenericSpecifications.containsTextInFields(search, "trackingCode", "customer.displayName")
```
*Equivalente conceptual Java:* `order.getTrackingCode().contains(search) || order.getCustomer().getDisplayName().contains(search)`

**SQL generado:**
```sql
LEFT JOIN users u ON order.customer_id = u.id
WHERE (LOWER(order.tracking_code) LIKE '%eric%' OR LOWER(u.display_name) LIKE '%eric%')
```

---

## Flujo Completo Paso a Paso

### Paso 1: Configurar el Service
Importa estáticamente `GenericSpecifications.*` y encadena tus condiciones usando `Specification.where(...)` y `.and(...)`:

```java
package com.andormix.swipemarketapi.order;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static com.andormix.swipemarketapi.common.util.GenericSpecifications.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findOrders(
            OrderStatus status, 
            String customerEmail, 
            String search
    ) {
        // Composición limpia de especificaciones
        Specification<Order> spec = Specification
                .<Order>where(isEqualTo("status", status))
                .and(joinIsEqualTo("customer", "email", customerEmail))
                .and(containsTextInFields(search, "trackingCode", "customer.displayName"));

        return orderRepository.findAll(spec)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
```

### Paso 2: Exponer el Endpoint en el Controller
Pasa los parámetros como `@RequestParam(required = false)`:

```java
@GetMapping
public List<OrderResponse> getOrders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String customerEmail,
        @RequestParam(required = false) String search
) {
    return orderService.findOrders(status, customerEmail, search);
}
```

---

## Matriz de Referencia para Nombres de Campo

| Tipo de Filtro | Atributo en Java | Sintaxis en GenericSpecifications |
| :--- | :--- | :--- |
| **Campo directo** | `Order.status` | `isEqualTo("status", value)` |
| **Relación 1 Nivel** | `Order.customer` $\rightarrow$ `User.email` | `joinIsEqualTo("customer", "email", value)` |
| **Búsqueda texto (Directo)** | `Order.trackingCode` | `containsTextInFields(search, "trackingCode")` |
| **Búsqueda texto (Anidado)** | `Order.customer` $\rightarrow$ `User.displayName` | `containsTextInFields(search, "customer.displayName")` |
| **Búsqueda texto (N Niveles)** | `ProductFavorite.product` $\rightarrow$ `Seller.name` | `containsTextInFields(search, "product.seller.name")` |
