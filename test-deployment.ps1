# Test deployment configuration for ODV Sicilia project
# This script validates the deployment setup for Render.com

Write-Host "=== ODV Sicilia Deployment Test ===" -ForegroundColor Green
Write-Host ""

# Check if required files exist
Write-Host "Checking required files..." -ForegroundColor Yellow

$requiredFiles = @(
    "Dockerfile",
    "render.yaml",
    "mvnw",
    "mvnw.cmd",
    ".mvn/wrapper/maven-wrapper.properties",
    "backend/pom.xml",
    "backend/src/main/java/it/odvsicilia/backend/BackendApplication.java"
)

$allFilesExist = $true
foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "✓ $file" -ForegroundColor Green
    } else {
        Write-Host "✗ $file - MISSING" -ForegroundColor Red
        $allFilesExist = $false
    }
}

Write-Host ""

# Check Dockerfile content
Write-Host "Checking Dockerfile configuration..." -ForegroundColor Yellow
$dockerfileContent = Get-Content "Dockerfile" -Raw

if ($dockerfileContent -match "FROM openjdk:17-jdk-slim") {
    Write-Host "✓ Correct Java 17 base image" -ForegroundColor Green
} else {
    Write-Host "✗ Incorrect Java version in Dockerfile" -ForegroundColor Red
    $allFilesExist = $false
}

if ($dockerfileContent -match "COPY --from=builder /app/target/\*\.jar app\.jar") {
    Write-Host "✓ Correct JAR file copy pattern" -ForegroundColor Green
} else {
    Write-Host "✗ JAR file copy pattern needs fixing" -ForegroundColor Red
    $allFilesExist = $false
}

# Check render.yaml configuration
Write-Host ""
Write-Host "Checking render.yaml configuration..." -ForegroundColor Yellow
$renderYamlContent = Get-Content "render.yaml" -Raw

if ($renderYamlContent -match "type: web") {
    Write-Host "✓ Web service type configured" -ForegroundColor Green
} else {
    Write-Host "✗ Web service type not configured" -ForegroundColor Red
    $allFilesExist = $false
}

if ($renderYamlContent -match "dockerfilePath: \./Dockerfile") {
    Write-Host "✓ Dockerfile path configured" -ForegroundColor Green
} else {
    Write-Host "✗ Dockerfile path not configured" -ForegroundColor Red
    $allFilesExist = $false
}

if ($renderYamlContent -match "healthCheckPath: /actuator/health") {
    Write-Host "✓ Health check path configured" -ForegroundColor Green
} else {
    Write-Host "✗ Health check path not configured" -ForegroundColor Red
    $allFilesExist = $false
}

# Check Maven wrapper
Write-Host ""
Write-Host "Checking Maven wrapper..." -ForegroundColor Yellow
if (Test-Path "mvnw") {
    Write-Host "✓ Maven wrapper script exists" -ForegroundColor Green
} else {
    Write-Host "✗ Maven wrapper script missing" -ForegroundColor Red
    $allFilesExist = $false
}

if (Test-Path ".mvn/wrapper/maven-wrapper.properties") {
    Write-Host "✓ Maven wrapper properties exist" -ForegroundColor Green
} else {
    Write-Host "✗ Maven wrapper properties missing" -ForegroundColor Red
    $allFilesExist = $false
}

# Check application properties
Write-Host ""
Write-Host "Checking application configuration..." -ForegroundColor Yellow
$appPropsContent = Get-Content "backend/src/main/resources/application.properties" -Raw

if ($appPropsContent -match "server\.port=\$\{PORT:8080\}") {
    Write-Host "✓ Server port configured for Render" -ForegroundColor Green
} else {
    Write-Host "✗ Server port not configured for Render" -ForegroundColor Red
    $allFilesExist = $false
}

if ($appPropsContent -match "spring\.datasource\.url=\$\{DATABASE_URL:") {
    Write-Host "✓ Database URL configuration present" -ForegroundColor Green
} else {
    Write-Host "✗ Database URL configuration missing" -ForegroundColor Red
    $allFilesExist = $false
}

# Check production properties
Write-Host ""
Write-Host "Checking production configuration..." -ForegroundColor Yellow
$prodPropsContent = Get-Content "backend/src/main/resources/application-prod.properties" -Raw

if ($prodPropsContent -match "spring\.datasource\.url=\$\{DATABASE_URL\}") {
    Write-Host "✓ Production database URL configured" -ForegroundColor Green
} else {
    Write-Host "✗ Production database URL not configured" -ForegroundColor Red
    $allFilesExist = $false
}

# Summary
Write-Host ""
Write-Host "=== Deployment Test Summary ===" -ForegroundColor Green
if ($allFilesExist) {
    Write-Host "✓ All deployment requirements met!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps for deployment:" -ForegroundColor Cyan
    Write-Host "1. Push your code to a Git repository (GitHub, GitLab, etc.)" -ForegroundColor White
    Write-Host "2. Connect your repository to Render.com" -ForegroundColor White
    Write-Host "3. Set the required environment variables in Render:" -ForegroundColor White
    Write-Host "   - DATABASE_URL (your PostgreSQL connection string)" -ForegroundColor White
    Write-Host "   - DB_USERNAME (database username)" -ForegroundColor White
    Write-Host "   - DB_PASSWORD (database password)" -ForegroundColor White
    Write-Host "   - BREVO_API_KEY (your Brevo email API key)" -ForegroundColor White
    Write-Host "   - ADMIN_EMAIL (admin email address)" -ForegroundColor White
    Write-Host "   - SUPABASE_ANON_KEY (Supabase anonymous key)" -ForegroundColor White
    Write-Host "   - SUPABASE_ROLE_KEY (Supabase service role key)" -ForegroundColor White
    Write-Host "4. Deploy using the render.yaml configuration" -ForegroundColor White
} else {
    Write-Host "✗ Some deployment requirements are missing!" -ForegroundColor Red
    Write-Host "Please fix the issues above before deploying." -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Environment Variables Required ===" -ForegroundColor Yellow
Write-Host "DATABASE_URL - PostgreSQL connection string" -ForegroundColor White
Write-Host "DB_USERNAME - Database username" -ForegroundColor White
Write-Host "DB_PASSWORD - Database password" -ForegroundColor White
Write-Host "BREVO_API_KEY - Brevo email service API key" -ForegroundColor White
Write-Host "ADMIN_EMAIL - Admin email address" -ForegroundColor White
Write-Host "SUPABASE_ANON_KEY - Supabase anonymous key" -ForegroundColor White
Write-Host "SUPABASE_ROLE_KEY - Supabase service role key" -ForegroundColor White
