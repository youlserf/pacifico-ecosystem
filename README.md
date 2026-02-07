# Pacífico Insurance Ecosystem

Sistema distribuido de alto rendimiento para la **emisión de pólizas vehiculares** integrando Inteligencia Artificial para el análisis de riesgo y procesamiento de eventos en tiempo real.

---

## 🏗️ Arquitectura de la Solución

La solución está diseñada bajo una arquitectura de microservicios distribuida, organizada en capas de responsabilidad clara:

1.  **Edge Layer**: 
    *   **API Gateway**: Punto de entrada único que gestiona la seguridad, el ruteo y la generación del `TraceID` inicial.
2.  **Control Plane**:
    *   **Discovery Server (Eureka)**: Registro dinámico de servicios.
    *   **Config Server**: Gestión centralizada de perfiles y propiedades.
3.  **Core Services**:
    *   **Quotation MS**: Orquestador de cotizaciones. Expone una interfaz **GraphQL**, utiliza **Virtual Threads (Project Loom)** para alta concurrencia y se comunica vía **gRPC** con el motor de riesgo.
    *   **ML Risk MS**: Servicio especializado en inferencia de riesgo mediante gRPC para asegurar latencias mínimas (<10ms).
    *   **Issuance MS**: Servicio encargado de la persistencia final y notificación vía **WebSockets**. Implementado en **Java 11** para demostrar compatibilidad con ecosistemas asíncronos mediante **Kafka**.
4.  **Data & Messaging**:
    *   **Persistencia**: PostgreSQL para datos transaccionales.
    *   **Caching**: Redis bajo el patrón **Cache-Aside** para optimizar el scoring de riesgo.
    *   **Event-Driven**: Kafka con **Avro** y **Schema Registry** para el desacoplamiento de contratos entre servicios.

---

## 🛠️ Stack Tecnológico

| Microservicio | Java | Framework | Responsabilidad |
| :--- | :--- | :--- | :--- |
| `api-gateway` | 21 | Spring Boot 3.1 | Ruteo, Auth & TraceID Generation |
| `quotation-ms` | 21 | Spring Boot 3.1 | GraphQL API, Virtual Threads, Orchestration |
| `ml-risk-ms` | 21 | Spring Boot 3.1 | gRPC Server, Risk ML Inference |
| `issuance-ms` | 11 | Spring Boot 2.7 | Kafka Consumer, WebSockets, Policy Generation |
| `discovery-server` | 21 | Spring Cloud | Service Registry |
| `config-server` | 21 | Spring Cloud | Centralized Configuration |

---

## 🔌 Contratos de Comunicación

### gRPC (ML Risk Inference)
Utilizamos gRPC binario para la comunicación síncrona entre el orquestador y el motor de IA:

```protobuf
service RiskInferenceService {
  rpc EvaluateRisk(RiskRequest) returns (RiskResponse);
}

message RiskRequest {
  string dni = 1;
  int32 age = 2;
  double car_value = 3;
}
```

### Kafka & Avro (Policy Events)
El desacoplamiento entre el Orquestador (Java 21) y el Emisor (Java 11) se garantiza mediante **Avro**. El **Schema Registry** actúa como validador de contratos, asegurando que la evolución de esquemas no rompa la compatibilidad entre productores y consumidores.

---

## 🛰️ Trazabilidad y Observabilidad

El sistema implementa el estándar **W3C Trace Context** para la propagación de contextos. Cada petición iniciada en el Gateway viaja con un `traceparent` único que se propaga a través de:
*   Protocolos HTTP (Gateway -> Quotation)
*   Protocolos Binarios (Quotation -> ML gRPC)
*   Sistemas de Mensajería (Quotation -> Kafka -> Issuance)

### Dashboards:
*   **Eureka Server**: [http://localhost:8761](http://localhost:8761)
*   **Zipkin (Tracing)**: [http://localhost:9411](http://localhost:9411)
*   **Config Server**: [http://localhost:8888/quotation-ms/default](http://localhost:8888/quotation-ms/default)

---

## 🚀 Guía de Inicio Rápido

### Requisitos
*   Docker & Docker Compose
*   Java 21 (para compilación local opcional)

### Despliegue
```bash
# Desde la raíz del proyecto
docker compose up -d
```

### Walkthrough Técnico
1.  **Conexión WebSocket**: Suscribirse a `ws://localhost:8083/ws/policy?dni=12345678` para recibir notificaciones en tiempo real.
2.  **Creación de Cotización**: Ejecutar la siguiente mutación en el Gateway (`http://localhost:8080/quotation/graphql`):

```graphql
mutation {
  createQuote(input: {
    dni: "12345678",
    age: 30,
    carValue: 25000.0
  }) {
    quoteId
    status
    message
  }
}
```

3.  **Verificación**: Consultar Zipkin para observar el flujo distribuido de la petición.

---

## 🧠 Decisiones de Diseño (The "Why")

*   **ML Service Separation**: Se aisló el motor de riesgo como un servicio gRPC independiente debido a su naturaleza computacional intensiva, permitiendo su escalado horizontal de forma elástica sin afectar la lógica de negocio.
*   **Pattern Cache-Aside (Redis)**: Implementado para los scores de riesgo. Dado que el perfil de riesgo de un cliente no cambia frecuentemente en periodos cortos, evitamos llamadas redundantes al motor de ML, reduciendo latencia y costos.
*   **JPA Entity Graphs**: Se utilizan `@EntityGraph` para resolver relaciones complejas en una sola consulta SQL, eliminando proactivamente el problema del **N+1** y optimizando el acceso a la base de datos de pólizas.
*   **Java 21 (Virtual Threads)**: El `quotation-ms` utiliza Virtual Threads para manejar miles de conexiones concurrentes de entrada (GraphQL) sin el overhead de los hilos de plataforma tradicionales.
