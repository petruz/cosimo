# CO.S.I.MO

A unified database query execution and debugging tool with a modern web interface. Execute SQL queries directly against PostgreSQL and ClickHouse databases without any modification or intermediate layers.

## Features

- 🚀 **Direct Query Execution** - Queries are executed WITHOUT MODIFICATION using native JDBC drivers
- 🗃️ **Multi-Database Support** - PostgreSQL and ClickHouse
- 🔌 **Connection Pooling** - HikariCP for efficient connection management
- 📊 **Interactive Results Grid** - Powered by AG-Grid with pagination and filtering
- 💻 **Monaco Editor** - Professional SQL editor with syntax highlighting
- 🔍 **Query Analysis** - EXPLAIN support for query optimization
- 🎯 **Zero Configuration** - Self-contained executable JAR

## Technology Stack

### Backend
- Java 17
- Spring Boot 3.3.4
- HikariCP 5.1.0
- PostgreSQL JDBC Driver 42.7.8
- ClickHouse JDBC Driver 0.6.5

### Frontend
- React 18
- Material-UI (MUI)
- Monaco Editor
- AG-Grid
- Vite

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Node.js 18+** (automatically managed by frontend-maven-plugin)
- **PostgreSQL** and/or **ClickHouse** database instance (for testing)

## Build

### Full Build (Backend + Frontend)

```bash
mvn clean package
```

This will:
1. Install Node.js and npm (if not present)
2. Install frontend dependencies
3. Build React frontend with Vite
4. Copy frontend assets to Spring Boot resources
5. Compile Java backend
6. Create executable JAR

### Backend Only

```bash
mvn clean compile
```

### Frontend Only

```bash
cd src/main/ui
npm install
npm run build
```

## Run

### Development Mode

Start the application with hot-reload support:

```bash
mvn spring-boot:run
```

The application will be available at: **http://localhost:8080**

### Production Mode

Run the standalone JAR:

```bash
java -jar target/query-debug-app-1.0.0-SNAPSHOT.jar
```

## Usage

1. **Open the application** in your browser at http://localhost:8080

2. **Connect to a database:**
   - Click "New Connection"
   - Select database type (PostgreSQL or ClickHouse)
   - Enter connection details:
     - Host (e.g., localhost)
     - Port (default: 5432 for PostgreSQL, 8123 for ClickHouse)
     - Database name
     - Username
     - Password
   - Click "Connect"

3. **Execute queries:**
   - Write your SQL query in the Monaco editor
   - Click "Execute" or press Ctrl+Enter
   - View results in the interactive grid

4. **Analyze queries:**
   - Click "Explain" to see the query execution plan
   - PostgreSQL: executes EXPLAIN ANALYZE
   - ClickHouse: executes EXPLAIN

## JDBC Drivers

The application supports loading JDBC drivers from two sources:

1. **Maven dependencies** (classpath) - specified in `pom.xml`
2. **Local JAR files** - place additional JDBC drivers in the `jdbc-drivers/` directory

Custom drivers will be automatically loaded at startup.

## Configuration

### Application Properties

Edit `src/main/resources/application.properties`:

```properties
server.port=8080
logging.level.com.debug.queryapp=INFO
```

### Connection Pool Settings

Connection pool settings are configured in the respective connection classes:
- `PostgresConnection.java`
- `ClickhouseConnection.java`

Default HikariCP settings:
- Maximum pool size: 10
- Minimum idle connections: 2
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes

## Project Structure

```
query-debug-app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/debug/queryapp/
│   │   │       ├── QueryAppApplication.java
│   │   │       ├── config/
│   │   │       ├── connection/         # Database connection management
│   │   │       ├── controller/         # REST API endpoints
│   │   │       ├── model/
│   │   │       └── service/
│   │   ├── resources/
│   │   │   ├── application.properties
│   │   │   └── static/                 # Built frontend assets
│   │   └── ui/                         # React frontend source
│   │       ├── src/
│   │       │   ├── components/
│   │       │   ├── context/
│   │       │   ├── hooks/
│   │       │   └── services/
│   │       ├── package.json
│   │       └── vite.config.js
├── jdbc-drivers/                       # Custom JDBC drivers
├── pom.xml
└── README.md
```

## API Endpoints

### Database Connection
- `POST /api/v1/database/connect` - Establish new database connection
- `GET /api/v1/database/connections` - List all connections
- `DELETE /api/v1/database/connections/{id}` - Close connection
- `GET /api/v1/database/metrics` - Get connection pool metrics

### Query Execution
- `POST /api/v1/query/execute` - Execute SELECT query
- `POST /api/v1/query/update` - Execute INSERT/UPDATE/DELETE statement
- `POST /api/v1/query/explain` - Execute EXPLAIN query

## Security Notes

⚠️ **Important:** This is a debugging tool intended for development environments only.

- No authentication/authorization implemented
- Credentials are not encrypted
- Direct database access without query validation
- Not suitable for production environments

## Development

### Frontend Development

For faster frontend development with hot-reload:

```bash
cd src/main/ui
npm install
npm run dev
```

This starts Vite dev server on http://localhost:5173 with proxy to backend.

### Backend Development

Run Spring Boot with dev tools:

```bash
mvn spring-boot:run
```

## Troubleshooting

### Build Issues

**Problem:** Frontend build fails
**Solution:** Clear npm cache and rebuild
```bash
cd src/main/ui
rm -rf node_modules package-lock.json
npm install
npm run build
```

**Problem:** Maven build fails
**Solution:** Clean and rebuild
```bash
mvn clean install -U
```

### Runtime Issues

**Problem:** Connection refused
**Solution:** Verify database is running and accessible at the specified host/port

**Problem:** Authentication failed
**Solution:** Check username and password credentials

**Problem:** JDBC driver not found
**Solution:** Ensure JDBC driver JARs are in `jdbc-drivers/` directory or added as Maven dependency

## License

This project is provided as-is for debugging and development purposes.

## Contributing

This is a personal debugging tool. Feel free to fork and customize for your needs.

## Support

For issues or questions, please refer to the source code documentation and inline comments.
