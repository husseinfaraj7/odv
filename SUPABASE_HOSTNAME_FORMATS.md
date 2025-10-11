# Supabase Database Hostname Formats

This document provides a comprehensive guide to valid Supabase database connection hostname patterns and common mistakes to avoid.

## Valid Connection Formats

Supabase provides three main connection modes, each with a specific hostname format and port.

### 1. Direct Connection Format

**Pattern**: `db.{project-ref}.supabase.co:5432`

**Use Case**: Direct connection to the database without pooling. Suitable for long-running processes, background jobs, and scripts that maintain persistent connections.

**Examples**:
```
db.abcdefghijklmnop.supabase.co:5432
db.xyzabc123456789.supabase.co:5432
db.myproject12345.supabase.co:5432
```

**DATABASE_URL Example**:
```
DATABASE_URL=jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?user=postgres.abcdefghijklmnop&password=your-password
```

### 2. Transaction Pooler Format (Connection Pooling Mode)

**Pattern**: `aws-0-{region}.pooler.supabase.com:6543`

**Use Case**: Recommended for serverless functions, API servers, and applications that create many short-lived connections. Uses PgBouncer in transaction mode.

**Regional Examples**:

**EU North 1 (Stockholm)**:
```
aws-0-eu-north-1.pooler.supabase.com:6543
```

**US East 1 (N. Virginia)**:
```
aws-0-us-east-1.pooler.supabase.com:6543
```

**AP Southeast 1 (Singapore)**:
```
aws-0-ap-southeast-1.pooler.supabase.com:6543
```

**DATABASE_URL Example**:
```
DATABASE_URL=jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?user=postgres.abcdefghijklmnop&password=your-password
```

### 3. Session Pooler Format

**Pattern**: `aws-0-{region}.pooler.supabase.com:5432`

**Use Case**: Connection pooling with session-level isolation. Maintains session state and supports prepared statements. Uses the same hostname as transaction pooler but with port 5432.

**Regional Examples**:

**EU North 1 (Stockholm)**:
```
aws-0-eu-north-1.pooler.supabase.com:5432
```

**US East 1 (N. Virginia)**:
```
aws-0-us-east-1.pooler.supabase.com:5432
```

**AP Southeast 1 (Singapore)**:
```
aws-0-ap-southeast-1.pooler.supabase.com:5432
```

**DATABASE_URL Example**:
```
DATABASE_URL=jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres?user=postgres.abcdefghijklmnop&password=your-password
```

## IPv6 Pooler Connections

**Important**: IPv6 pooler connections use the **same hostname and port** as the IPv4 transaction pooler:

**Pattern**: `aws-0-{region}.pooler.supabase.com:6543`

The pooler infrastructure automatically handles both IPv4 and IPv6 traffic on port 6543.

## Common Hostname Mistakes

### ❌ Malformed Pattern: `db.aws-*`

**Incorrect Example**:
```
db.aws-0-eu-north-1.pooler.supabase.com:5432
```

**Why This Fails**:
This hostname pattern incorrectly combines the direct connection prefix (`db.`) with the pooler hostname format (`aws-0-{region}.pooler.supabase.com`). This creates a malformed hostname that does not exist in DNS, resulting in:

```
java.net.UnknownHostException: db.aws-0-eu-north-1.pooler.supabase.com
```

**Root Cause**: The `db.` prefix is **only** used with project-specific direct connections (`db.{project-ref}.supabase.co`), **never** with pooler hostnames.

### Side-by-Side Comparison

#### Scenario 1: Direct Connection

| Status | Format | Port | Explanation |
|--------|--------|------|-------------|
| ❌ **Incorrect** | `db.aws-0-eu-north-1.pooler.supabase.com` | 5432 | Combines `db.` prefix with pooler hostname |
| ✅ **Correct** | `db.abcdefghijklmnop.supabase.co` | 5432 | Uses project reference with `db.` prefix |

#### Scenario 2: Transaction Pooler

| Status | Format | Port | Explanation |
|--------|--------|------|-------------|
| ❌ **Incorrect** | `db.aws-0-eu-north-1.pooler.supabase.com` | 6543 | Should not use `db.` prefix with pooler |
| ✅ **Correct** | `aws-0-eu-north-1.pooler.supabase.com` | 6543 | Pooler hostname without `db.` prefix |

#### Scenario 3: Session Pooler

| Status | Format | Port | Explanation |
|--------|--------|------|-------------|
| ❌ **Incorrect** | `db.aws-0-eu-north-1.pooler.supabase.com` | 5432 | Should not use `db.` prefix with pooler |
| ✅ **Correct** | `aws-0-eu-north-1.pooler.supabase.com` | 5432 | Pooler hostname without `db.` prefix |

### Other Common Mistakes

#### Missing or Incorrect Region

❌ **Incorrect**:
```
aws-0-europe.pooler.supabase.com:6543
aws-0.pooler.supabase.com:6543
```

✅ **Correct**:
```
aws-0-eu-north-1.pooler.supabase.com:6543
```

**Note**: Always use the full AWS region identifier (e.g., `eu-north-1`, not `europe` or `eu`).

#### Wrong Port for Connection Type

❌ **Incorrect**:
```
# Using transaction pooler port for direct connection
db.abcdefghijklmnop.supabase.co:6543

# Using direct connection port for transaction pooler
aws-0-eu-north-1.pooler.supabase.com:5432  # This is session pooler, not transaction pooler
```

✅ **Correct**:
```
# Direct connection uses port 5432
db.abcdefghijklmnop.supabase.co:5432

# Transaction pooler uses port 6543
aws-0-eu-north-1.pooler.supabase.com:6543

# Session pooler uses port 5432
aws-0-eu-north-1.pooler.supabase.com:5432
```

## How to Construct Your Database URL

### Step 1: Extract Your Project Reference ID

1. Log in to the [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Settings** → **Database**
4. Find the **Connection String** section
5. Your project reference is the string in the format `postgres.{project-ref}`

**Example**: If your connection string shows `postgres.abcdefghijklmnop`, your project reference is `abcdefghijklmnop`.

### Step 2: Identify Your Region

In the same **Settings** → **Database** page:
1. Look for **Region** information
2. Note the AWS region (e.g., `eu-north-1`, `us-east-1`, `ap-southeast-1`)

### Step 3: Choose Your Connection Mode

#### Direct Connection (Best for: Long-running processes, batch jobs)

```
DATABASE_URL=jdbc:postgresql://db.{project-ref}.supabase.co:5432/postgres?user=postgres.{project-ref}&password={your-password}
```

**Real Example**:
```
DATABASE_URL=jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?user=postgres.abcdefghijklmnop&password=MySecurePass123!
```

#### Transaction Pooler (Best for: Serverless, API servers, high concurrency)

```
DATABASE_URL=jdbc:postgresql://aws-0-{region}.pooler.supabase.com:6543/postgres?user=postgres.{project-ref}&password={your-password}
```

**Real Example**:
```
DATABASE_URL=jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?user=postgres.abcdefghijklmnop&password=MySecurePass123!
```

#### Session Pooler (Best for: Applications needing session state, prepared statements)

```
DATABASE_URL=jdbc:postgresql://aws-0-{region}.pooler.supabase.com:5432/postgres?user=postgres.{project-ref}&password={your-password}
```

**Real Example**:
```
DATABASE_URL=jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:5432/postgres?user=postgres.abcdefghijklmnop&password=MySecurePass123!
```

## When to Use Each Connection Type

| Connection Type | Use When | Avoid When |
|----------------|----------|------------|
| **Direct Connection** | • Long-running background jobs<br>• Database migrations<br>• Admin tasks<br>• Low connection count | • Serverless functions<br>• High-concurrency APIs<br>• Short-lived connections |
| **Transaction Pooler** | • Serverless functions (Lambda, Cloud Functions)<br>• Stateless API servers<br>• Microservices<br>• High connection turnover | • Applications using prepared statements<br>• Applications requiring session variables<br>• Long transactions |
| **Session Pooler** | • Applications using prepared statements<br>• Applications requiring session state<br>• ORM frameworks (Hibernate, JPA)<br>• Connection pooling with session isolation | • Maximum connection efficiency is critical<br>• Truly stateless operations |

## Quick Reference

### Hostname Format Summary

| Connection Type | Hostname Pattern | Port | Example |
|----------------|------------------|------|---------|
| Direct | `db.{project-ref}.supabase.co` | 5432 | `db.abc123.supabase.co:5432` |
| Transaction Pooler | `aws-0-{region}.pooler.supabase.com` | 6543 | `aws-0-eu-north-1.pooler.supabase.com:6543` |
| Session Pooler | `aws-0-{region}.pooler.supabase.com` | 5432 | `aws-0-eu-north-1.pooler.supabase.com:5432` |
| IPv6 Pooler | `aws-0-{region}.pooler.supabase.com` | 6543 | `aws-0-us-east-1.pooler.supabase.com:6543` |

### Regional Pooler Hostnames

| Region | AWS Region Code | Pooler Hostname |
|--------|----------------|-----------------|
| Stockholm | eu-north-1 | `aws-0-eu-north-1.pooler.supabase.com` |
| N. Virginia | us-east-1 | `aws-0-us-east-1.pooler.supabase.com` |
| Singapore | ap-southeast-1 | `aws-0-ap-southeast-1.pooler.supabase.com` |
| São Paulo | sa-east-1 | `aws-0-sa-east-1.pooler.supabase.com` |
| Sydney | ap-southeast-2 | `aws-0-ap-southeast-2.pooler.supabase.com` |
| Tokyo | ap-northeast-1 | `aws-0-ap-northeast-1.pooler.supabase.com` |
| London | eu-west-2 | `aws-0-eu-west-2.pooler.supabase.com` |
| Frankfurt | eu-central-1 | `aws-0-eu-central-1.pooler.supabase.com` |

## Troubleshooting

### UnknownHostException

If you encounter `java.net.UnknownHostException`, verify:

1. ✅ You're not mixing `db.` prefix with pooler hostnames
2. ✅ The region code is correct and complete (e.g., `eu-north-1`, not `eu`)
3. ✅ The hostname format matches one of the valid patterns exactly
4. ✅ There are no typos in `supabase.co` or `pooler.supabase.com`

### Connection Refused

If you can resolve the hostname but cannot connect:

1. ✅ Verify the port matches the connection type (5432 for direct/session, 6543 for transaction)
2. ✅ Check that your IP is allowed in Supabase project settings
3. ✅ Ensure your credentials are correct (username format: `postgres.{project-ref}`)

### Connection Timeouts or Pool Exhaustion

If you experience connection pool issues:

1. ✅ Consider switching from direct connection to transaction pooler
2. ✅ Ensure you're closing connections properly in your application
3. ✅ Review your connection pool settings in Spring Boot configuration
