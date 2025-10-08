# Security - Credential Management Best Practices

This document outlines security best practices for managing credentials in the ODV Sicilia backend application. For deployment-specific procedures and troubleshooting, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Table of Contents
- [Environment Variable Naming Conventions](#environment-variable-naming-conventions)
- [Configuring Credentials in Render Dashboard](#configuring-credentials-in-render-dashboard)
- [Protecting Credentials from Git](#protecting-credentials-from-git)
- [Credential Rotation Procedures](#credential-rotation-procedures)
- [Pre-Commit Security Checklist](#pre-commit-security-checklist)

---

## Environment Variable Naming Conventions

The application uses environment variables for all sensitive credentials and configuration. All variables follow the naming patterns established in `.env.example` and are externalized in `application-prod.properties` using Spring's `${VARIABLE_NAME}` syntax.

### Required Credentials

| Variable | Purpose | Format | Example |
|----------|---------|--------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://user:password@host:port/database` | `postgresql://postgres.pejuystijjkjxjctieyb:MyP@ss123@aws-1-eu-north-1.pooler.supabase.com:6543/postgres` |
| `DATABASE_USER` | Database username | `postgres.[PROJECT_REF]` | `postgres.pejuystijjkjxjctieyb` |
| `DATABASE_PASSWORD` | Database password (raw, not URL-encoded) | Any secure password | `MyP@ss123!` |
| `BREVO_API_KEY` | Brevo email service API key | `xkeysib-[64 hex chars]-[suffix]` | `xkeysib-a1b2c3d4...` |
| `BREVO_SMTP_USERNAME` | Brevo SMTP username | Email-like format | `96f5ae002@smtp-brevo.com` |
| `SUPABASE_URL` | Supabase project URL | `https://[PROJECT_REF].supabase.co` | `https://pejuystijjkjxjctieyb.supabase.co` |
| `SUPABASE_ANON_KEY` | Supabase anonymous key | JWT token | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |
| `SUPABASE_ROLE_KEY` | Supabase service role key | JWT token | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |

### Optional Configuration

| Variable | Purpose | Default |
|----------|---------|---------|
| `ADMIN_EMAIL` | Admin email for notifications | `ussofaraj@gmail.com` |
| `PORT` | Server port | `8080` |
| `DDL_AUTO` | Hibernate schema mode | `validate` (prod) / `update` (dev) |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `FRONTEND_URL` | CORS allowed frontend URL | `http://localhost:3000` (dev) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | Production domains |
| `H2_CONSOLE_ENABLED` | Enable H2 console (dev only) | `false` (prod) / `true` (dev) |

### Naming Convention Rules

1. **Use UPPER_SNAKE_CASE** for all environment variables
2. **Prefix by component**: `DATABASE_*`, `BREVO_*`, `SUPABASE_*`
3. **Use descriptive names**: `DATABASE_URL` not `DB_URL`, `BREVO_API_KEY` not `API_KEY`
4. **Match Spring property names**: `DATABASE_URL` maps to `${DATABASE_URL}` in properties files
5. **Avoid abbreviations** unless they're standard (e.g., `URL`, `API`, `SMTP`)

### Credential Externalization Pattern

All credentials are externalized in `application-prod.properties` using Spring's property placeholder syntax:

```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
brevo.api.key=${BREVO_API_KEY}
spring.mail.username=${BREVO_SMTP_USERNAME}
spring.mail.password=${BREVO_API_KEY}
```

This pattern is also followed in `render.yaml` for deployment configuration.

---

## Configuring Credentials in Render Dashboard

Render provides a secure dashboard interface for managing environment variables. All credentials should be configured through Render's dashboard or CLI, never hardcoded in the repository.

### Step-by-Step Configuration Process

#### Step 1: Access Your Service

1. Log in to [Render Dashboard](https://dashboard.render.com)
2. Navigate to **Services** in the left sidebar
3. Click on your service name: **faraj-project-backend**

#### Step 2: Navigate to Environment Settings

1. In the service detail page, click the **Environment** tab in the left sidebar
2. You'll see a list of existing environment variables

#### Step 3: Add Individual Environment Variables

1. Click the **Add Environment Variable** button
2. Enter the **Key** (e.g., `DATABASE_PASSWORD`)
3. Enter the **Value** (the actual credential)
4. Click **Save Changes**

**Important**: When you save changes, Render will automatically redeploy your service.

#### Step 4: Using Secret Files (Alternative Method)

For bulk credential management:

1. Click **Add Secret File** button
2. Enter the filename: `.env`
3. Paste the content in `.env` format:
   ```
   DATABASE_PASSWORD=your_password
   BREVO_API_KEY=your_api_key
   SUPABASE_ANON_KEY=your_anon_key
   ```
4. Click **Save**

**Note**: Secret files are mounted as files in the container filesystem, while environment variables are set in the process environment. This project uses environment variables exclusively.

#### Step 5: Managing Synced Variables

Some variables in `render.yaml` use `sync: true`:

```yaml
- key: SUPABASE_ANON_KEY
  sync: true
```

For synced variables:
1. The variable must be set in **Render's Environment Groups** or **manually in the dashboard**
2. Synced variables are shared across multiple services
3. To create an Environment Group:
   - Go to **Account Settings** → **Environment Groups**
   - Create a new group (e.g., "Production Secrets")
   - Add variables to the group
   - Link the group to your service

### Render Secret Management Features

#### Visibility Controls
- **Hidden by default**: Credential values are masked in the dashboard
- **Click "Show"** to temporarily reveal values for verification
- **Audit logs**: Render tracks who accessed/modified variables (available on paid plans)

#### Variable Precedence
1. **Environment tab variables** (highest priority)
2. **Environment Groups**
3. **render.yaml default values** (lowest priority)

#### Best Practices
- ✅ Use descriptive variable names matching your `.env.example`
- ✅ Set `sync: true` for shared credentials (database, external APIs)
- ✅ Document which variables are required in `render.yaml` comments
- ✅ Test changes in a preview environment before production
- ❌ Never set placeholder values like `YOUR_PASSWORD` in production
- ❌ Don't include credentials in `render.yaml` `value:` fields

### Verifying Configuration

After setting variables:

1. Check the **Logs** tab during deployment
2. Look for successful connection messages:
   ```
   INFO  - HikariPool-1 - Start completed.
   INFO  - Started BackendApplication in 4.235 seconds
   ```
3. Test the health endpoint: `curl https://your-service.onrender.com/actuator/health`
4. Verify email functionality by triggering a contact form submission

---

## Protecting Credentials from Git

**CRITICAL**: Never commit credentials to version control. All `.env` files (except `.env.example`) must be excluded from Git.

### Gitignore Verification

#### Check Current Gitignore Configuration

Your `.gitignore` should contain these patterns:

```gitignore
# Environment files - exclude all .env files
.env
.env.*
backend/.env
backend/.env.*
frontend/.env
frontend/.env.*

# Environment file exceptions - include template files
!.env.example
!.env.render.example
!backend/.env.example
!backend/.env.render.example
!frontend/.env.example
!frontend/.env.render.example
```

#### Verification Commands

**Verify .env files are ignored:**
```powershell
# Test if .env would be ignored
git check-ignore .env
# Expected output: .env

# Test backend .env
git check-ignore backend/.env
# Expected output: backend/.env
```

**List all ignored files in your working directory:**
```powershell
git status --ignored
```

**Find any .env files that might be tracked:**
```powershell
git ls-files | Select-String "\.env"
```

**Expected output**: Only `.env.example` files should appear. If you see `.env` without `.example`, it means an actual `.env` file is tracked in Git.

#### Removing Accidentally Committed Credentials

If you accidentally committed a `.env` file with credentials:

**1. Remove from Git (keep local copy):**
```powershell
git rm --cached .env
git rm --cached backend/.env
git commit -m "Remove accidentally committed .env files"
```

**2. Verify removal:**
```powershell
git ls-files | Select-String "\.env"
# Should only show .env.example files
```

**3. IMMEDIATELY rotate all exposed credentials:**
- Change all passwords in Supabase
- Regenerate all API keys in Brevo
- Update credentials in Render dashboard
- See [Credential Rotation Procedures](#credential-rotation-procedures) below

**4. Consider the repository compromised:**
- If pushed to GitHub/GitLab, credentials are permanently in Git history
- Use `git filter-repo` or `BFG Repo-Cleaner` to remove from history (advanced)
- Notify your team about the security incident

### Pre-Push Safety Checks

Before every `git push`, run:

```powershell
# Check for staged .env files
git diff --cached --name-only | Select-String "\.env"

# If any non-.example .env files appear, unstage them:
git restore --staged path/to/.env
```

---

## Credential Rotation Procedures

Regular credential rotation is a security best practice. Rotate credentials when:
- A team member with access leaves
- You suspect credentials may have been exposed
- As part of regular security maintenance (quarterly recommended)
- After any security incident

### Rotating Supabase Database Credentials

The Supabase database credentials consist of the project reference (`pejuystijjkjxjctieyb`) and database password.

#### Step 1: Generate New Password in Supabase

1. Log in to [Supabase Dashboard](https://app.supabase.com)
2. Select your project: **pejuystijjkjxjctieyb**
3. Navigate to **Settings** → **Database**
4. Scroll to **Database Password** section
5. Click **Generate a new password** or **Reset database password**
6. **IMPORTANT**: Copy the new password immediately (it won't be shown again)

**Security Note**: The new password takes effect immediately. Your application will lose database connectivity until you update the credentials in Render.

#### Step 2: Update DATABASE_URL in Render

The `DATABASE_URL` includes the embedded password. You must update it with the new password.

**Current format:**
```
postgresql://postgres.pejuystijjkjxjctieyb:[OLD_PASSWORD]@aws-1-eu-north-1.pooler.supabase.com:6543/postgres
```

**New format (with new password):**
```
postgresql://postgres.pejuystijjkjxjctieyb:[NEW_PASSWORD]@aws-1-eu-north-1.pooler.supabase.com:6543/postgres
```

**Update in Render Dashboard:**

1. Go to Render Dashboard → **faraj-project-backend** → **Environment**
2. Find `DATABASE_URL` and click **Edit**
3. Replace `[OLD_PASSWORD]` with `[NEW_PASSWORD]` in the connection string
4. Click **Save Changes**

**Update DATABASE_PASSWORD (if set separately):**

1. Find `DATABASE_PASSWORD` variable
2. Click **Edit**
3. Enter the new password (raw, not URL-encoded)
4. Click **Save Changes**

**Using Render CLI (Alternative):**
```powershell
# Set new DATABASE_URL
render env set DATABASE_URL "postgresql://postgres.pejuystijjkjxjctieyb:[NEW_PASSWORD]@aws-1-eu-north-1.pooler.supabase.com:6543/postgres"

# Set new DATABASE_PASSWORD
render env set DATABASE_PASSWORD "[NEW_PASSWORD]"
```

#### Step 3: Verify Connection

1. Render will automatically redeploy your service
2. Monitor the **Logs** tab for successful connection:
   ```
   INFO  - HikariPool-1 - Starting...
   INFO  - HikariPool-1 - Start completed.
   ```
3. Test the health endpoint:
   ```powershell
   curl https://faraj-project-backend.onrender.com/actuator/health
   ```
4. Verify database operations work (e.g., create a test order)

#### Zero-Downtime Rotation (Advanced)

For zero-downtime rotation, you would need:
1. Supabase to support multiple valid passwords simultaneously (not currently supported)
2. Blue-green deployment strategy
3. Connection pool graceful shutdown

**Current limitation**: Brief downtime (30-60 seconds) is expected during Render redeployment.

### Rotating Brevo API Keys

Brevo API keys are used for sending emails via SMTP.

#### Step 1: Generate New API Key in Brevo

1. Log in to [Brevo Dashboard](https://app.brevo.com)
2. Navigate to **SMTP & API** → **API Keys**
3. Click **Generate a new API key**
4. Enter a name: `ODV Sicilia Backend - [DATE]`
5. Click **Generate**
6. **IMPORTANT**: Copy the API key immediately (format: `xkeysib-[64 hex chars]-[suffix]`)

#### Step 2: Update BREVO_API_KEY in Render

**Via Render Dashboard:**

1. Go to Render Dashboard → **faraj-project-backend** → **Environment**
2. Find `BREVO_API_KEY` and click **Edit**
3. Paste the new API key
4. Click **Save Changes**

**Via Render CLI:**
```powershell
render env set BREVO_API_KEY "xkeysib-your-new-key-here"
```

#### Step 3: Test Email Functionality

1. Wait for Render redeployment to complete
2. Test the contact form on your frontend
3. Verify email delivery:
   - Check admin email inbox for contact form submission
   - Check Brevo Dashboard → **Logs** → **Email Logs** for sent messages

4. Monitor application logs for email errors:
   ```powershell
   # Check recent logs for email sending
   render logs --service faraj-project-backend --tail
   ```

#### Step 4: Revoke Old API Key

Only after confirming the new key works:

1. Return to **Brevo Dashboard** → **SMTP & API** → **API Keys**
2. Find the old API key
3. Click **Delete** or **Revoke**
4. Confirm revocation

**Best Practice**: Keep the old key active for 24-48 hours after deploying the new one, in case rollback is needed.

### Rotating Supabase Anon/Role Keys

Supabase JWT keys (SUPABASE_ANON_KEY, SUPABASE_ROLE_KEY) are rotated differently than database passwords.

#### When to Rotate

- These are JWT signing keys tied to your project
- Rotation requires generating new JWT secrets in Supabase project settings
- Less frequent rotation (annually or after compromise)

#### Rotation Process

1. Go to **Supabase Dashboard** → **Settings** → **API**
2. Under **Project API keys**, you'll see:
   - `anon` public key
   - `service_role` secret key
3. These keys are derived from JWT Secret (not directly rotatable through UI)
4. To rotate, you must regenerate the JWT Secret:
   - Contact Supabase support for project JWT secret rotation
   - Or create a new project and migrate data

**Note**: JWT key rotation is a major operation and should be done during planned maintenance.

---

## Pre-Commit Security Checklist

Run these checks before every commit to ensure no credentials are accidentally included.

### Quick Security Scan

Run all checks at once:

```powershell
# Create a security scan script
$securityCheck = @"
Write-Host "🔒 Running security scan..." -ForegroundColor Cyan

# Check 1: Supabase project reference
Write-Host "`n📋 Check 1: Supabase project reference" -ForegroundColor Yellow
grep -r "pejuystijjkjxjctieyb" --exclude-dir=target --exclude-dir=.git --exclude="*.md" --exclude="render.yaml" .
if (`$LASTEXITCODE -eq 1) { Write-Host "✅ No exposed project references" -ForegroundColor Green }

# Check 2: Brevo API keys
Write-Host "`n📋 Check 2: Brevo API keys" -ForegroundColor Yellow
grep -r "xkeysib-" --exclude-dir=target --exclude-dir=.git .
if (`$LASTEXITCODE -eq 1) { Write-Host "✅ No API keys found" -ForegroundColor Green }

# Check 3: JWT tokens
Write-Host "`n📋 Check 3: JWT tokens (Supabase keys)" -ForegroundColor Yellow
grep -r "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" --exclude-dir=target --exclude-dir=.git .
if (`$LASTEXITCODE -eq 1) { Write-Host "✅ No JWT tokens found" -ForegroundColor Green }

# Check 4: Database passwords in URLs
Write-Host "`n📋 Check 4: Database credentials in connection strings" -ForegroundColor Yellow
grep -r "postgresql://.*:.*@" --exclude-dir=target --exclude-dir=.git --exclude="*.md" --exclude="render.yaml" .
if (`$LASTEXITCODE -eq 1) { Write-Host "✅ No embedded credentials in URLs" -ForegroundColor Green }

# Check 5: .env files
Write-Host "`n📋 Check 5: .env files in working directory" -ForegroundColor Yellow
`$envFiles = Get-ChildItem -Recurse -Filter ".env" | Where-Object { `$_.Name -notlike "*.example" -and `$_.Name -notlike "*.render.example" }
if (`$envFiles.Count -eq 0) {
    Write-Host "✅ No .env files in working directory" -ForegroundColor Green
} else {
    Write-Host "⚠️ WARNING: Found .env files:" -ForegroundColor Red
    `$envFiles | ForEach-Object { Write-Host "  - `$(`$_.FullName)" -ForegroundColor Red }
}

# Check 6: Staged .env files
Write-Host "`n📋 Check 6: Staged files check" -ForegroundColor Yellow
`$stagedEnvFiles = git diff --cached --name-only | Select-String "\.env`$"
if (`$null -eq `$stagedEnvFiles) {
    Write-Host "✅ No .env files staged for commit" -ForegroundColor Green
} else {
    Write-Host "⚠️ WARNING: .env files staged for commit:" -ForegroundColor Red
    `$stagedEnvFiles | ForEach-Object { Write-Host "  - `$_" -ForegroundColor Red }
}

Write-Host "`n🔒 Security scan complete!" -ForegroundColor Cyan
"@

$securityCheck | Out-File -FilePath "security-check.ps1" -Encoding UTF8
```

Then run:
```powershell
.\security-check.ps1
```

### Individual Check Commands

#### Check 1: Supabase Project Reference

Scan for the Supabase project reference `pejuystijjkjxjctieyb`:

```powershell
# Recursive search (excluding documentation and config)
grep -r "pejuystijjkjxjctieyb" --exclude-dir=target --exclude-dir=.git --exclude="*.md" --exclude="render.yaml" .

# Expected: No results (or only in SECURITY.md, render.yaml)
```

**Allowed locations:**
- `render.yaml` (deployment config)
- `SECURITY.md` (this documentation)
- `DEPLOYMENT.md` (deployment guide)

**Not allowed:**
- Application code (`.java`, `.properties`)
- `.env` files
- Frontend code

#### Check 2: Brevo API Keys

Scan for Brevo API key pattern `xkeysib-`:

```powershell
# Search for Brevo API keys
grep -r "xkeysib-" --exclude-dir=target --exclude-dir=.git .

# Expected: No results
```

**If found**: Remove immediately, regenerate API key, update Render dashboard.

#### Check 3: JWT Tokens (Supabase Keys)

Scan for JWT token pattern (SUPABASE_ANON_KEY, SUPABASE_ROLE_KEY):

```powershell
# Search for JWT tokens
grep -r "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" --exclude-dir=target --exclude-dir=.git .

# Expected: No results
```

**Note**: JWT tokens typically start with `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9` (base64-encoded `{"alg":"HS256","typ":"JWT"}`).

#### Check 4: Database Credentials in URLs

Scan for passwords embedded in PostgreSQL connection strings:

```powershell
# Search for connection strings with embedded credentials
grep -r "postgresql://.*:.*@" --exclude-dir=target --exclude-dir=.git --exclude="*.md" --exclude="render.yaml" .

# Also check for postgres:// variant
grep -r "postgres://.*:.*@" --exclude-dir=target --exclude-dir=.git --exclude="*.md" --exclude="render.yaml" .

# Expected: No results (credentials should only be in environment variables)
```

**Allowed patterns:**
```
# ✅ Property placeholder (correct)
spring.datasource.url=${DATABASE_URL}

# ✅ Documentation example (with placeholder)
postgresql://postgres.pejuystijjkjxjctieyb:[YOUR-PASSWORD]@aws-1-eu-north-1.pooler.supabase.com:6543/postgres

# ❌ Actual password (wrong)
postgresql://postgres.pejuystijjkjxjctieyb:MyP@ss123@aws-1-eu-north-1.pooler.supabase.com:6543/postgres
```

#### Check 5: Find .env Files in Working Directory

Locate any `.env` files that shouldn't exist:

```powershell
# Find all .env files
Get-ChildItem -Recurse -Filter ".env*" | Where-Object { 
    $_.Name -notlike "*.example" -and 
    $_.Name -notlike "*.render.example" 
} | Select-Object FullName

# Expected: No results
```

#### Check 6: Verify .env Files Are Gitignored

```powershell
# Test if .env would be ignored
git check-ignore .env backend/.env frontend/.env

# Expected output:
# .env
# backend/.env
# frontend/.env

# If no output, .env files are NOT gitignored (fix .gitignore)
```

#### Check 7: Scan for Hardcoded Passwords

Look for common password patterns:

```powershell
# Search for potential hardcoded passwords
Select-String -Path "backend\src\main\**\*.java" -Pattern "(password|passwd|pwd)\s*=\s*['\"]" -Exclude "*Test*.java"

# Expected: No results in production code (test fixtures are okay)
```

### Automated Pre-Commit Hook

Create a Git pre-commit hook to automatically run security checks:

```powershell
# Create .git/hooks/pre-commit file
$hookContent = @'
#!/bin/sh
echo "🔒 Running pre-commit security checks..."

# Check for staged .env files
staged_env=$(git diff --cached --name-only | grep -E "\.env$" | grep -v ".env.example")
if [ -n "$staged_env" ]; then
    echo "❌ ERROR: .env files are staged for commit:"
    echo "$staged_env"
    echo ""
    echo "To fix: git restore --staged <file>"
    exit 1
fi

# Check for API keys
if git diff --cached | grep -E "xkeysib-[a-f0-9]{64}"; then
    echo "❌ ERROR: Brevo API key detected in staged changes!"
    exit 1
fi

# Check for JWT tokens
if git diff --cached | grep -E "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"; then
    echo "❌ ERROR: JWT token detected in staged changes!"
    exit 1
fi

# Check for connection strings with passwords
if git diff --cached | grep -E "postgresql://[^:]+:[^@]+@" | grep -v "\[.*PASSWORD.*\]"; then
    echo "❌ ERROR: Database connection string with password detected!"
    exit 1
fi

echo "✅ Pre-commit security checks passed!"
exit 0
'@

$hookContent | Out-File -FilePath ".git\hooks\pre-commit" -Encoding UTF8 -NoNewline

# Make executable (on Unix systems)
# chmod +x .git/hooks/pre-commit
```

### False Positive Exceptions

Some files legitimately contain pattern examples:

**Allowed files:**
- `SECURITY.md` - This security documentation
- `DEPLOYMENT.md` - Deployment guide with examples
- `render.yaml` - Deployment config with placeholder values
- `.env.example` - Template with placeholder values

**Verification**: These files should only contain placeholders, not real credentials:
- `[YOUR-PASSWORD]` ✅
- `MyActualPassword123` ❌

### What to Do If You Find Exposed Credentials

**Immediate actions:**

1. **DO NOT COMMIT**: If credentials are unstaged, remove them immediately
   ```powershell
   git restore <file>
   ```

2. **If already committed locally** (not pushed):
   ```powershell
   # Amend the commit
   git add <fixed-file>
   git commit --amend --no-edit
   ```

3. **If already pushed to remote**:
   - **IMMEDIATELY** rotate all exposed credentials (see [Credential Rotation](#credential-rotation-procedures))
   - Remove from Git history using `git filter-repo` or `BFG Repo-Cleaner`
   - Force push (coordinate with team)
   - Consider the repository compromised

4. **Document the incident**:
   - What was exposed
   - When it was exposed
   - What actions were taken
   - Lessons learned

---

## Additional Resources

- [DEPLOYMENT.md](DEPLOYMENT.md) - Full deployment guide with troubleshooting
- [Render Documentation - Environment Variables](https://render.com/docs/environment-variables)
- [Supabase Documentation - Database Passwords](https://supabase.com/docs/guides/database/managing-passwords)
- [Brevo API Documentation](https://developers.brevo.com/)
- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)

---

**Last Updated**: 2024
**Maintained By**: ODV Sicilia Development Team
