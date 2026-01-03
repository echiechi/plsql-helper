# PL/SQL Tools - Annotation-Based Oracle Procedure/Function Call Generator

A Java annotation processing library that automatically generates type-safe Oracle PL/SQL procedure and function calls, eliminating boilerplate JDBC code and providing compile-time validation.

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Detailed Usage](#detailed-usage)
  - [1. Define Your Service Interface](#1-define-your-service-interface)
  - [2. Define Record Classes](#2-define-record-classes)
  - [3. Automatic Implementation Generation](#3-automatic-implementation-generation)
  - [4. Use the Generated Implementation](#4-use-the-generated-implementation)
- [Annotations Reference](#annotations-reference)
- [Advanced Features](#advanced-features)
- [How It Works](#how-it-works)
- [Building the Project](#building-the-project)
- [Requirements](#requirements)
- [Examples](#examples)

## Overview

**PL/SQL Tools** is a compile-time annotation processor that transforms abstract Java methods into fully implemented Oracle database calls. Instead of writing repetitive JDBC boilerplate, you define your database interface using annotations, and the library generates all the necessary code during compilation.

**Before (Traditional JDBC):**
```java
public Integer insertCustomer(String firstName, String lastName, String email) {
    String sql = "{ call pkg_customer_management.insert_customer(p_first_name => ?, p_last_name => ?, p_email => ?, p_customer_id => ?) }";
    try (Connection conn = dataSource.getConnection();
         CallableStatement stmt = conn.prepareCall(sql)) {

        stmt.setString(1, firstName);
        stmt.setString(2, lastName);
        stmt.setString(3, email);
        stmt.registerOutParameter(4, Types.INTEGER);

        stmt.execute();

        return stmt.getInt(4);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
```

**After (PL/SQL Tools):**
```java
@Package(name = "pkg_customer_management")
public abstract class CustomerService extends DataSourceAware {
    public CustomerService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @PlsqlCallable(name = "insert_customer", dataSource = DataSources.MY_DS,
                   outputs = @Output("p_customer_id"))
    public abstract Integer insertCustomer(
        @PlsqlParam("p_first_name") String firstName,
        @PlsqlParam("p_last_name") String lastName,
        @PlsqlParam("p_email") String email
    );
}
```

The implementation is **automatically generated** during compilation!

## Key Features

- **Zero Boilerplate**: Write abstract methods, get full implementations
- **Compile-Time Safety**: Errors caught during compilation, not runtime
- **Type-Safe Mapping**: Automatic Java ↔ JDBC type conversion
- **Object Flattening**: Nested objects automatically flattened to procedure parameters
- **ResultSet Mapping**: Automatic mapping from cursors to Java objects
- **Multiple Outputs**: Support for procedures with multiple OUT parameters
- **Named Parameters**: Oracle named parameter syntax (`param => ?`)
- **Connection Management**: Built-in connection handling and resource cleanup
- **Function Support**: Both procedures and functions supported
- **Optional Results**: `Optional<T>` support for nullable results
- **Collection Support**: `List<T>` for multiple rows

## Quick Start

### 1. Add Dependencies

Add to your `pom.xml`:

```xml
<dependencies>
    <!-- Core annotations and runtime -->
    <dependency>
        <groupId>com.plsql.tools</groupId>
        <artifactId>plsql-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

    <!-- Oracle JDBC driver -->
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc11</artifactId>
        <version>23.4.0.24.05</version>
    </dependency>
</dependencies>
```

Configure annotation processing:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.11.0</version>
            <configuration>
                <source>17</source>
                <target>17</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.plsql.tools</groupId>
                        <artifactId>plsql-gen</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </path>
                    <path>
                        <groupId>com.plsql.tools</groupId>
                        <artifactId>plsql-core</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. Define Your Service

```java
import com.plsql.tools.DataSourceAware;
import com.plsql.tools.DataSourceProvider;
import com.plsql.tools.annotations.*;

@Package(name = "pkg_customer_management")
public abstract class CustomerService extends DataSourceAware {
    public CustomerService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @PlsqlCallable(name = "insert_customer", dataSource = "MY_DS",
                   outputs = @Output("p_customer_id"))
    public abstract Integer insertCustomer(
        @PlsqlParam("p_first_name") String firstName,
        @PlsqlParam("p_last_name") String lastName
    );

    @PlsqlCallable(name = "get_customer_by_id", dataSource = "MY_DS",
                   outputs = @Output("p_customer_data"))
    public abstract Optional<CustomerGet> getCustomerById(
        @PlsqlParam("p_customer_id") long id
    );
}
```

### 3. Compile and Use

```bash
mvn clean compile
```

The annotation processor generates `CustomerServiceImpl` automatically:

```java
// Usage
var oracleDataSource = new OracleDataSource();
oracleDataSource.setURL("jdbc:oracle:thin:@//localhost:1521/FREEPDB1");

var dsProvider = new DefaultDataSourceProvider();
dsProvider.registerDataSource("MY_DS", oracleDataSource);

CustomerService service = new CustomerServiceImpl(dsProvider);

// Call methods - implementation is auto-generated!
Integer customerId = service.insertCustomer("John", "Doe");
Optional<CustomerGet> customer = service.getCustomerById(customerId);
```

## Project Structure

The project consists of three Maven modules:

```
plsql-tools/
├── plsql-core/          # Core annotations and runtime utilities
│   ├── @Package         # Annotation for service classes
│   ├── @PlsqlCallable   # Marks methods for generation
│   ├── @Record          # Marks classes for object mapping
│   ├── @PlsqlParam      # Maps parameters to PL/SQL names
│   ├── @Output          # Marks OUT parameters
│   ├── DataSourceProvider # DataSource management
│   └── TypeMapper       # Java ↔ JDBC type conversion
│
├── plsql-gen/           # Annotation processor (code generator)
│   ├── PLSQLAnnotationProcessor
│   ├── CallableGenerator
│   └── StringTemplate generators
│
└── plsql-e2e/           # End-to-end examples
    └── Examples of service classes and usage
```

## Detailed Usage

### 1. Define Your Service Interface

Create an abstract class extending `DataSourceAware` and annotate it with `@Package`:

```java
@Package(name = "pkg_customer_management")
public abstract class CustomerService extends DataSourceAware {
    public CustomerService(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    // Define abstract methods here
}
```

**Service Class Requirements:**
- Must extend `DataSourceAware`
- Must have a constructor accepting `DataSourceProvider`
- Methods must be `abstract`
- Annotate with `@Package` to specify the Oracle package name

### 2. Define Record Classes

Use `@Record` to mark classes that will be used for parameter objects or result mapping:

**Simple Record:**
```java
@Data
@Record
public class CustomerInsert {
    @PlsqlParam("p_first_name")
    private String firstName;

    @PlsqlParam("p_last_name")
    private String lastName;

    @PlsqlParam("p_email")
    private String email;

    @PlsqlParam("p_age")
    private Integer age;
}
```

**Nested Record (Automatic Flattening):**
```java
@Data
@Record
public class ComposedCustomerInsert {
    @PlsqlParam("p_first_name")
    private String firstName;

    @PlsqlParam("p_email")
    private String email;

    // Nested record - fields are automatically flattened
    CustomerAddress customerAddress;
}

@Data
@Record
public class CustomerAddress {
    @PlsqlParam("p_address_line1")
    private String addressLine1;

    @PlsqlParam("p_city")
    private String city;

    @PlsqlParam("p_postal_code")
    private String postalCode;
}
```

When you use `ComposedCustomerInsert` as a parameter, all fields from both classes are flattened:
```java
@PlsqlCallable(name = "insert_customer", dataSource = "MY_DS",
               outputs = @Output("p_customer_id"))
public abstract Integer insertCustomer(ComposedCustomerInsert customer);

// Generated code accesses nested fields:
// customer.getFirstName() → p_first_name
// customer.getEmail() → p_email
// customer.getCustomerAddress().getAddressLine1() → p_address_line1
// customer.getCustomerAddress().getCity() → p_city
```

**Result Record (for ResultSet mapping):**
```java
@Data
@Record
public class CustomerGet {
    private Long customerId;                // Maps from CUSTOMER_ID column
    private String firstName;               // Maps from FIRST_NAME column
    private String lastName;                // Maps from LAST_NAME column
    private String email;                   // Maps from EMAIL column
    private LocalDateTime registrationDate; // Maps from REGISTRATION_DATE column
    private char isActive;                  // Maps from IS_ACTIVE column
}
```

Column names are automatically converted from `snake_case` to `camelCase`.

### 3. Automatic Implementation Generation

Define abstract methods with annotations:

**Simple Procedure with OUT Parameter:**
```java
@PlsqlCallable(name = "insert_customer", dataSource = "MY_DS",
               outputs = @Output("p_customer_id"))
public abstract Integer insertCustomer(
    @PlsqlParam("p_first_name") String firstName,
    @PlsqlParam("p_last_name") String lastName,
    @PlsqlParam("p_email") String email
);
```

**Procedure with No Return (void):**
```java
@PlsqlCallable(name = "update_customer", dataSource = "MY_DS")
public abstract void updateCustomer(
    @PlsqlParam("p_customer_id") long id,
    @PlsqlParam("p_first_name") String firstName,
    @PlsqlParam("p_last_name") String lastName
);
```

**Procedure Returning Single Row:**
```java
@PlsqlCallable(name = "get_customer_by_id", dataSource = "MY_DS",
               outputs = @Output("p_customer_data"))
public abstract Optional<CustomerGet> getCustomerById(
    @PlsqlParam("p_customer_id") long id
);
```

**Procedure Returning Multiple Rows:**
```java
@PlsqlCallable(name = "get_customers_by_city", dataSource = "MY_DS",
               outputs = @Output("p_customer_cursor"))
public abstract List<CustomerGet> getCustomersByCity(
    @PlsqlParam("p_city") String city
);
```

**Function Call:**
```java
@PlsqlCallable(
    name = "get_customer_full_name",
    dataSource = "MY_DS",
    type = CallableType.FUNCTION,
    outputs = @Output("customer_full_name")
)
public abstract String getCustomerFullName(
    @PlsqlParam("p_customer_id") long id
);
```

**Multiple OUT Parameters:**
```java
@Data
@Record
public class CustomerMulti {
    private long customerTotal;
    private List<CustomerGet> customerGets;
}

@PlsqlCallable(name = "get_all_customers", dataSource = "MY_DS",
    outputs = {
        @Output(value = "p_customer_cursor", field = "customerGets"),
        @Output(value = "p_total_count", field = "customerTotal")
    })
public abstract CustomerMulti getAllCustomers(
    @PlsqlParam("p_page_size") int pageSize,
    @PlsqlParam("p_page_number") int pageNumber
);
```

### 4. Use the Generated Implementation

**Setup DataSource:**
```java
// Create Oracle DataSource
var oracleDataSource = new OracleDataSource();
oracleDataSource.setURL("jdbc:oracle:thin:@//localhost:1521/FREEPDB1");
oracleDataSource.setUser("username");
oracleDataSource.setPassword("password");

// Register with provider
var dsProvider = new DefaultDataSourceProvider();
dsProvider.registerDataSource("MY_DS", oracleDataSource);
```

**Use Generated Service:**
```java
// Instantiate the generated implementation
CustomerService service = new CustomerServiceImpl(dsProvider);

// Call procedures/functions as regular Java methods
Integer customerId = service.insertCustomer("John", "Doe", "john@example.com");
System.out.println("Created customer with ID: " + customerId);

Optional<CustomerGet> customer = service.getCustomerById(customerId);
customer.ifPresent(c -> System.out.println("Customer: " + c.getFirstName()));

List<CustomerGet> customers = service.getCustomersByCity("Los Angeles");
System.out.println("Found " + customers.size() + " customers");

String fullName = service.getCustomerFullName(customerId);
System.out.println("Full name: " + fullName);
```

## Annotations Reference

### @Package
**Target:** Type (Class)
**Purpose:** Marks a service class and specifies the Oracle package name

```java
@Package(
    name = "pkg_customer_management",  // Oracle package name
    schema = "HR",                     // Optional schema
    datasource = "MY_DS"               // Default datasource for all methods
)
```

### @PlsqlCallable
**Target:** Method
**Purpose:** Marks an abstract method for implementation generation

```java
@PlsqlCallable(
    name = "insert_customer",           // PL/SQL procedure/function name
    dataSource = "MY_DS",               // Required: datasource identifier
    type = CallableType.PROCEDURE,      // PROCEDURE or FUNCTION
    outputs = @Output("p_customer_id")  // OUT parameter definitions
)
```

### @Function
**Target:** Method
**Purpose:** Simplified annotation for functions (must have return value)

```java
@Function(
    name = "calculate_total",  // Function name
    dataSource = "MY_DS"       // Datasource identifier
)
```

### @Record
**Target:** Type (Class)
**Purpose:** Marks a class for object-to-parameter flattening or ResultSet mapping

```java
@Record
@Data  // Lombok for getters/setters
public class Customer {
    // Fields
}
```

### @PlsqlParam
**Target:** Parameter, Field
**Purpose:** Maps Java parameter/field to PL/SQL parameter name

```java
@PlsqlParam("p_customer_id")  // Oracle parameter name
private Long customerId;
```

### @Output
**Target:** Field, Type
**Purpose:** Marks OUT or IN/OUT parameters

```java
@Output(
    value = "p_customer_cursor",  // OUT parameter name
    field = "customerGets"        // Field name in result object (for multiple outputs)
)
```

### @MultiOutput
**Target:** Type
**Purpose:** Container for multiple @Output annotations

```java
@MultiOutput({
    @Output(value = "p_cursor", field = "customers"),
    @Output(value = "p_total", field = "total")
})
```

## Advanced Features

### Type Mapping

Automatic conversion between Java and JDBC types:

| Java Type | JDBC Type | Notes |
|-----------|-----------|-------|
| `String` | `VARCHAR` | Standard string mapping |
| `Integer`, `int` | `INTEGER` | 32-bit integers |
| `Long`, `long` | `BIGINT` | 64-bit integers |
| `Double`, `double` | `DOUBLE` | Floating point |
| `BigDecimal` | `NUMERIC` | Precise decimals |
| `LocalDate` | `DATE` | Date only |
| `LocalDateTime` | `TIMESTAMP` | Date and time |
| `LocalTime` | `TIME` | Time only |
| `char` | `CHAR` | Single character |
| `Boolean`, `boolean` | `BOOLEAN` | True/false |

### Naming Conventions

Automatic conversion between Java and Oracle naming:

```java
// Java camelCase → Oracle snake_case
getCustomerById  → get_customer_by_id
firstName        → first_name
isActive         → is_active

// Oracle snake_case → Java camelCase (ResultSet mapping)
CUSTOMER_ID      → customerId
FIRST_NAME       → firstName
IS_ACTIVE        → isActive
```

### Connection Management

The library handles all connection management automatically:

```java
// Generated code includes proper resource management:
try (Connection cnx = ds.getConnection();
     CallableStatement stmt = cnx.prepareCall(sql)) {
    // Execute procedure
    stmt.execute();
    // Extract results
} catch (SQLException e) {
    throw new RuntimeException(e);
}
```

- Connections are automatically closed
- Statements are automatically closed
- Exceptions are wrapped in `RuntimeException`

### DataSource Providers

**Default Provider:**
```java
var dsProvider = new DefaultDataSourceProvider();
dsProvider.registerDataSource("MY_DS", oracleDataSource);
dsProvider.registerDataSource("BACKUP_DS", backupDataSource);
```

**Custom Provider:**
```java
public class CustomDataSourceProvider implements DataSourceProvider {
    @Override
    public DataSource getDataSource(String name) {
        // Custom logic to retrieve DataSource
        return lookupDataSource(name);
    }
}
```

## How It Works

### Compilation Flow

1. **Write Code**: Define abstract service classes with annotations
2. **Compile**: Run `mvn compile`
3. **Annotation Processing**:
   - Pass 1: Process all `@Record` classes, cache their structure
   - Pass 2: Process all `@Package` classes
   - For each `@PlsqlCallable` method:
     - Extract method signature
     - Extract parameters and their types
     - Extract return type
     - Generate JDBC code using StringTemplate
4. **Code Generation**: Create `*Impl` classes in `target/generated-sources/annotations/`
5. **Compilation**: Generated code is compiled with your code
6. **Result**: Use generated implementations as regular Java classes

### Generated Code Structure

For this abstract method:
```java
@PlsqlCallable(name = "insert_customer", dataSource = "MY_DS",
               outputs = @Output("p_customer_id"))
public abstract Integer insertCustomer(
    @PlsqlParam("p_first_name") String firstName,
    @PlsqlParam("p_last_name") String lastName
);
```

The processor generates:
```java
public class CustomerServiceImpl extends CustomerService {
    public static final String pkg_customer_management_insert_customer =
        "{ call pkg_customer_management.insert_customer(p_first_name => ?, p_last_name => ?, p_customer_id => ?) }";

    @Override
    public Integer insertCustomer(String firstName, String lastName) {
        DataSource ds = dataSourceProvider.getDataSource("MY_DS");

        try (Connection cnx = ds.getConnection();
             CallableStatement stmt = cnx.prepareCall(pkg_customer_management_insert_customer)) {

            int pos = 1;
            stmt.setString(pos++, firstName);
            stmt.setString(pos++, lastName);
            stmt.registerOutParameter(pos, JDBCType.INTEGER);

            stmt.execute();

            Integer result = stmt.getInt(pos);
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
```

## Building the Project

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Oracle Database (for running examples)

### Build Commands

```bash
# Clean and build all modules
mvn clean install

# Build specific module
cd plsql-core && mvn clean install
cd plsql-gen && mvn clean install
cd plsql-e2e && mvn clean install

# Skip tests
mvn clean install -DskipTests

# View generated sources
ls plsql-e2e/target/generated-sources/annotations/
```

### Build Order

The modules must be built in this order:
1. **plsql-core** - Contains annotations (no processing)
2. **plsql-gen** - Contains processor (compiled with `-proc:none`)
3. **plsql-e2e** - Examples (annotation processing enabled)

### IDE Setup

**IntelliJ IDEA:**
1. Import as Maven project
2. Enable annotation processing: Settings → Build → Compiler → Annotation Processors
3. Generated sources automatically added to classpath

**Eclipse:**
1. Import as Maven project
2. Install M2E APT plugin
3. Enable annotation processing in project properties

## Requirements

### Runtime Requirements
- Java 17+
- Oracle JDBC Driver (ojdbc11)
- Oracle Database 11g+

### Development Requirements
- Maven 3.6+
- StringTemplate 4 (ST4) - for code generation
- Apache Commons Lang3 - for utilities

### Optional
- Lombok - for reducing boilerplate in record classes
- JUnit 5 - for testing

## Examples

See the `plsql-e2e` module for complete working examples:

- **CustomerService**: Basic procedures, functions, and result mapping
- **CustomerService2**: Nested object flattening
- **OtherTestsService**: Advanced scenarios
- **Main.java**: Complete usage example with Oracle database

### Running the Examples

1. Set up Oracle database:
```sql
-- Create the package and procedures (SQL scripts not included in README)
```

2. Update connection settings in `Main.java`:
```java
public static final String url = "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";
public static final String userName = "YOUR_USERNAME";
public static final String password = "YOUR_PASSWORD";
```

3. Run:
```bash
mvn clean install
cd plsql-e2e
mvn exec:java -Dexec.mainClass="com.plsql.tools.Main"
```

## Current Status

**Version:** 1.0.0-SNAPSHOT (Proof of Concept)

**Working Features:**
- Basic procedure/function generation
- Parameter binding (simple types and objects)
- Object flattening (nested records)
- ResultSet mapping (single row, multiple rows, Optional)
- Multiple OUT parameters
- Named parameter syntax
- Type mapping and conversion

**Known Limitations:**
- Primitive return types need refinement
- Stream-based ResultSet processing not yet implemented
- Limited error handling customization
- Testing coverage needs expansion

## Contributing

This is a proof-of-concept project. Contributions, issues, and feature requests are welcome!

## License

[Specify your license here]

---

**Need Help?**

Check the `plsql-e2e` module for comprehensive examples, or review the annotation processor source code in `plsql-gen` for implementation details.
