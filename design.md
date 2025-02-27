# Version Management for os-core-lib-azure

This document outlines the design principles and dependency management strategy for the `os-core-lib-azure` library. It describes the dependency hierarchy, the role of BOMs (Bill of Materials), and the approach to ensure consistent versioning and conflict resolution. The `os-core-lib-azure` library acts as the central module to govern dependency versions for itself and its foundational library, `os-core-common`.

## Version Table

| Component | Version | Component | Version |
|-----------|---------|-----------|---------|
| Spring Framework | 6.2.0 | Spring Boot | 3.4.0 |
| Azure SDK BOM | 1.2.30 | Azure Spring Boot | 5.18.0 |
| Microsoft Graph | 6.23.0 | `App Insights` | `3.6.2` |
| `Service Bus` | `3.6.7` | `Event Grid` | `1.4.0` |
| `Event Grid Management` | `1.0.0-beta-4` | Micrometer | 1.14.1 |
| Jakarta Servlet | 6.0.0 | Jakarta Inject | 2.0.1 |
| JSON | 20231013 | Log4j SLF4J | 2.24.2 |
| Resilience4j | 2.0.0 | Redisson | 3.40.2 |
| Guava | 33.3.1-jre | | |

> **Note:** `Legacy` dependencies that need to be migrated and are Vulnerability Patched.

## Version Update Process

### Update Strategy by Component Level

1. __Service/Project Level (Highest)__
   - __When to use:__
     - Emergency security patches needed before library updates are available
     - Service-specific dependency requirements
     - Temporary overrides while waiting for upstream library updates
   - __Process:__
     - Document override in service's POM with reason and timeline
     - Create tracking issue for removing override once upstream is updated
     - Test thoroughly with service's test suite

2. __os-core-lib-azure (Central Version Management)__
   - __When to use:__
     - Primary location for all version updates including:
       - Core Java libraries
       - Framework versions (Spring, Jakarta, etc.)
       - Azure-specific dependencies
       - Security patches affecting multiple services
       - Version upgrades needed across all services
   - __Process:__
     - Update version in properties section
     - Document security fixes with CVE references
     - Test integration with target services
     - Ensure additional services are updated within a reasonable timeframe

> **Note:** While `os-core-common` provides foundational functionality, all version management is centralized in `os-core-lib-azure`. This ensures consistent versioning across the platform and simplifies dependency management.


## Version Management Design

### 1. `os-core-common` Library

__Purpose:__ Acts as the foundational library in the dependency chain.

__Integration__: Imported in `os-core-lib-azure` using the dependencyManagement section.

__Transitive dependencies:__

- Spring Core libraries (e.g., spring-core, spring-context) with provided scope
- Common utility libraries (e.g., commons-lang3, guava)
- Logging libraries (e.g., log4j or slf4j)

#### Key Design Notes:

- `os-core-common` includes transitive dependencies but does __not__ dictate their versions.
- Final versions are controlled by BOMs included in `os-core-lib-azure`.
- Some versions of libraries (e.g., `gson`, `lettuce-core`, `commons-codec`) are overridden directly in `os-core-common`, but the final resolution is governed by `os-core-lib-azure`.

__Example POM Configuration:__
```xml
<dependency>
    <groupId>org.opengroup.osdu</groupId>
    <artifactId>os-core-common</artifactId>
    <version>${osdu.oscorecommon.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 2. `os-core-lib-azure` Library

__Purpose:__ Builds upon os-core-common and manages centralized versions.

__Dependencies Introduced:__

- Azure SDK libraries (e.g., azure-core, azure-identity)
- Azure Spring Boot integrations (e.g., spring-cloud-azure-starter)
- Additional Spring Framework modules (e.g., spring-web, spring-data)
- Dependency exclusions to resolve conflicts (e.g., excluding commons-logging)

__Relevant BOMs:__

- `spring-framework-bom` for specified version.
- `spring-boot-dependencies` for specified version.
- `azure-sdk-bom` for specified version.
- `spring-cloud-azure-dependencies` for specified version.


__Dependency Version Overrides:__

- Transitive dependencies from `os-core-common` (e.g., `spring-core`, `spring-context`, `lettuce-core`) are overridden using the BOMs in `os-core-lib-azure`.
- Explicit overrides from `os-core-common` (e.g., `gson`, `commons-codec`) are respected unless further overridden by `os-core-lib-azure`.


__Example POM Dependency Management:__
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-framework-bom</artifactId>
            <version>${spring-framework.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot-dependencies.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.azure</groupId>
            <artifactId>azure-sdk-bom</artifactId>
            <version>${azure-sdk-bom.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## Dependency Diagram

To visualize the relationships:

```bash
Root Project
├── os-core-lib-azure (Primary Library)
│   ├── Dependency Management (Manages BOMs and direct dependencies)
│   │   ├── Spring Framework BOM
│   │   ├── Spring Boot BOM
│   │   └── Azure SDK BOM
│   │
│   └── os-core-common
│
├── Spring Framework
├── Azure SDK Libraries
└── Miscellaneous Libraries (e.g., Resilience4j, Micrometer, Redisson)
```

## Key Notes:

### Order of Dependency Resolution:
- The dependencyManagement section ensures consistent versions across libraries by declaring versions explicitly
- Dependencies declared in the `<dependencies>` section of a module take precedence over transitive dependencies
- If multiple versions of a library are included transitively, Maven resolves to the nearest dependency in the tree (depth-first resolution)

### Order Matters:
- If os-core-lib-azure imports os-core-common and includes its own version of log4j, the os-core-lib-azure version will override os-core-common's version
- BOMs listed earlier in the dependencyManagement section may define default versions for transitive dependencies, impacting downstream modules

### Practical Impact in This Project:
- In this design, os-core-lib-azure sets the versions of spring-core and spring-context via the BOMs, ensuring that os-core-common uses these versions without introducing its own
- This centralized control eliminates version drift and guarantees alignment with project requirements

### Potential Conflicts:
- Conflicts may arise if dependencies from different BOMs have overlapping libraries with mismatched versions (e.g., Guava, Jakarta APIs)
- Use exclusions judiciously to avoid redundant dependencies

### Recommendations for Further Clarity

1. Explicitly document all versions overridden in os-core-lib-azure and their rationale
2. Regularly audit BOM versions to address security vulnerabilities and maintain compatibility
3. Minimize overrides in os-core-common to reduce complexity in managing dependency versions

# Dependency Resolution Details

#### BOM Import Order and Direct Dependency Management

The order of imports in the dependencyManagement section is crucial:

1. __BOMs__

   - `spring-framework-bom` (Primary Spring Framework versions)
   - `azure-sdk-bom` (Azure SDK library versions)
   - `spring-cloud-azure-dependencies` (Azure Spring integration versions)
   - `spring-boot-dependencies` (Spring Boot and related dependencies)
   - `os-core-common` (Base library dependencies)

2. __Direct Dependency Management__

   - `springdoc-openapi-starter-webmvc-ui` (Managed directly as it doesn't provide a BOM)

This order ensures proper version resolution with Spring Framework versions taking precedence, while still maintaining control over non-BOM dependencies through direct version management.

#### Version Management Categories

1. __BOM-Managed Versions__

   - Spring Framework components (via `spring-framework-bom`)
   - Azure SDK components (via `azure-sdk-bom`)
   - Azure Spring integrations (via `spring-cloud-azure-dependencies`)
   - Spring Boot and related dependencies (via `spring-boot-dependencies`)
   - Base library dependencies (via `os-core-common`)

   _Notable version management through these BOMs:_

   - Spring Framework core libraries
   - Azure SDK client libraries
   - Azure Spring integrations
   - Spring Boot starters and dependencies
   - Lombok (managed by spring-cloud-azure-dependencies)
   - Test dependencies (JUnit, Mockito via spring-framework-bom)
   - Common utility libraries inherited from os-core-common

2. __Explicitly Versioned Dependencies__

    - Azure Libraries:
      - `microsoft-graph`: ${microsoft-graph.version}
      - `azure.appinsights`: ${azure.appinsights.version}
    - Legacy Azure Libraries:
      - `azure-servicebus`: ${azure-servicebus.version}
      - `azure-eventgrid`: ${azure-eventgrid.version}
      - `azure-mgmt-eventgrid`: ${azure-mgmt-eventgrid.version}
    - Monitoring and Metrics:
      - `io.micrometer`: ${io.micrometer.version}
    - Jakarta APIs:
      - `jakarta.servlet-api`: ${jakarta.servlet.version}
     - jakarta.inject-api: ${jakarta.inject.version}
   - Utility Libraries:
     - `json`: ${json.version}
     - `log4j-slf4j-impl`: ${log4j-slf4j-impl.version}
     - `resilience4j`: ${resilience4j.version}
     - `redisson`: ${redisson.version}
     - `lettuce`: ${lettuce.version}

__Legacy Azure Services__

The project includes some end-of-life Azure services with planned migration paths:
- `azure-servicebus` → `com.azure:azure-messaging-servicebus`
- `azure-eventgrid` → `com.azure:azure-messaging-eventgrid`

These dependencies require special handling and security patches until migration is complete.

