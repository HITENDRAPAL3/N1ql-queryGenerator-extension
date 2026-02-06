# N1QL Query Generator - IntelliJ Plugin

A visual, form-based IntelliJ plugin that helps developers generate Couchbase N1QL queries without needing to memorize syntax. Perfect for developers new to Couchbase or those who want to quickly generate correct N1QL syntax.

## Features

### Supported Operations
- **SELECT** - Query documents with field selection, WHERE conditions, ORDER BY, LIMIT/OFFSET
- **INSERT** - Insert new documents with custom keys or auto-generated UUIDs
- **UPDATE** - Update existing documents with SET clauses
- **DELETE** - Delete documents with WHERE conditions
- **UPSERT** - Insert or update documents

### WHERE Clause Builder
- Dynamic condition rows with multiple operators
- Supported operators: `=`, `!=`, `>`, `<`, `>=`, `<=`, `LIKE`, `IN`, `IS NULL`, `IS NOT NULL`, `BETWEEN`, `CONTAINS`
- Logical connectors: AND, OR
- **Subquery support** for complex nested queries

### Output Actions
- **Copy to Clipboard** - One-click copy of generated query
- **Insert at Cursor** - Insert directly into active editor
- **Format Query** - Pretty-print with line breaks and indentation

## Screenshots

```
┌──────────────────────────────────────────────────────────────────┐
│  N1QL Query Generator                                            │
├──────────────────────────────────────────────────────────────────┤
│  📦 Bucket Name:  [users        ]                                │
│  📁 Scope:        [_default     ]                                │
│  📄 Collection:   [profiles     ]                                │
├──────────────────────────────────────────────────────────────────┤
│  Operation:  (●) SELECT  ( ) INSERT  ( ) UPDATE  ( ) DELETE      │
├──────────────────────────────────────────────────────────────────┤
│  [✓] SELECT *    [ ] DISTINCT                                    │
│  Fields: [name, email, age]                                      │
├──────────────────────────────────────────────────────────────────┤
│  WHERE Conditions:                                               │
│    [status] [=] [active]  [AND]                                  │
│    [age   ] [>=] [25    ]                                        │
│                                          [+ Add Condition]       │
├──────────────────────────────────────────────────────────────────┤
│  ORDER BY: [created_at] [DESC]           [+ Add Sort Field]      │
│  LIMIT: [100]     OFFSET: [0]                                    │
├──────────────────────────────────────────────────────────────────┤
│  Generated Query:                                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ SELECT *                                                   │  │
│  │ FROM `users`.`_default`.`profiles`                         │  │
│  │ WHERE status = "active" AND age >= 25                      │  │
│  │ ORDER BY created_at DESC                                   │  │
│  │ LIMIT 100 OFFSET 0                                         │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                   │
│  [📋 Copy to Clipboard]  [📝 Insert at Cursor]  [🔄 Reset]       │
└──────────────────────────────────────────────────────────────────┘
```

## Installation

### From ZIP File
1. Download the latest release ZIP file
2. In IntelliJ IDEA, go to **Settings** → **Plugins** → **⚙️** → **Install Plugin from Disk...**
3. Select the downloaded ZIP file
4. Restart IntelliJ IDEA

### From Source
1. Clone this repository
2. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
3. The plugin ZIP will be in `build/distributions/`
4. Install from disk as described above

## Usage

1. Open the tool window: **View** → **Tool Windows** → **N1QL Query Generator**
   - Or use keyboard shortcut: `Ctrl+Alt+Q`
2. Enter your bucket, scope, and collection names
3. Select the operation type (SELECT, INSERT, UPDATE, DELETE, UPSERT)
4. Fill in the operation-specific fields
5. Add WHERE conditions as needed
6. The query is generated in real-time in the preview area
7. Click **Copy to Clipboard** or **Insert at Cursor**

## Building from Source

### Prerequisites
- JDK 17 or higher
- Gradle 8.5 or higher (or use the included wrapper)

### Build Commands

```bash
# Build the plugin
./gradlew buildPlugin

# Run IntelliJ IDEA with the plugin installed (for testing)
./gradlew runIde

# Run tests
./gradlew test

# Clean build
./gradlew clean buildPlugin
```

## Project Structure

```
couchbase-query-generator/
├── src/main/
│   ├── java/n1ql/query/generator/
│   │   ├── actions/
│   │   │   └── OpenQueryBuilderAction.java
│   │   ├── builder/
│   │   │   └── N1QLQueryBuilder.java
│   │   ├── model/
│   │   │   ├── QueryModel.java
│   │   │   ├── QueryOperation.java
│   │   │   ├── WhereCondition.java
│   │   │   ├── WhereOperator.java
│   │   │   ├── LogicalOperator.java
│   │   │   ├── SortOrder.java
│   │   │   ├── OrderByClause.java
│   │   │   └── SetClause.java
│   │   └── ui/
│   │       ├── QueryBuilderToolWindowFactory.java
│   │       ├── QueryBuilderPanel.java
│   │       └── components/
│   │           ├── WhereClausePanel.java
│   │           ├── OrderByPanel.java
│   │           ├── SetClausePanel.java
│   │           └── SubqueryDialog.java
│   └── resources/
│       ├── META-INF/plugin.xml
│       └── icons/couchbase.svg
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Supported N1QL Syntax

### SELECT
```sql
SELECT [DISTINCT] field1, field2, ...
FROM `bucket`.`scope`.`collection`
WHERE condition1 AND/OR condition2 ...
ORDER BY field ASC/DESC
LIMIT n OFFSET m
```

### INSERT
```sql
INSERT INTO `bucket`.`scope`.`collection`
(KEY, VALUE)
VALUES ("document-key", {"field": "value"})
RETURNING *
```

### UPDATE
```sql
UPDATE `bucket`.`scope`.`collection`
SET field1 = value1, field2 = value2
WHERE condition
RETURNING *
```

### DELETE
```sql
DELETE FROM `bucket`.`scope`.`collection`
WHERE condition
RETURNING *
```

### UPSERT
```sql
UPSERT INTO `bucket`.`scope`.`collection`
(KEY, VALUE)
VALUES ("document-key", {"field": "value"})
RETURNING *
```

## Requirements

- IntelliJ IDEA 2023.3 or later
- Java 17 or later

## License

This project is open source.

## Support

For issues and feature requests, please contact support.
